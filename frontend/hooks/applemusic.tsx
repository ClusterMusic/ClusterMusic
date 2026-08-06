import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react'
import { Song } from './player';
import { Platform } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import { makeRedirectUri, useAuthRequest, ResponseType } from 'expo-auth-session';
import * as AppleAuthentication from 'expo-apple-authentication';

WebBrowser.maybeCompleteAuthSession();

const MUSICKIT_DEVELOPER_TOKEN = process.env.EXPO_PUBLIC_MUSICKIT_DEVELOPER_TOKEN || '';
const MUSICKIT_APP_NAME = process.env.EXPO_PUBLIC_MUSICKIT_APP_NAME || 'Cluster';
const MUSICKIT_APP_BUILD = process.env.EXPO_PUBLIC_MUSICKIT_APP_BUILD || '1.0.0';
const APPLE_TEAM_ID = process.env.EXPO_PUBLIC_APPLE_TEAM_ID || '';
const APPLE_SERVICE_ID = process.env.EXPO_PUBLIC_APPLE_SERVICE_ID || '';

const APPLE_MUSIC_API_BASE = 'https://api.music.apple.com';

interface AppleMusicSession {
    musicUserToken: string;
    isAuthorized: boolean;
}

interface MusicKitInstance {
    authorize: () => Promise<string>;
    unauthorize: () => Promise<void>;
    isAuthorized: boolean;
    musicUserToken: string;
    play: () => Promise<void>;
    pause: () => void;
    stop: () => void;
    skipToNextItem: () => Promise<void>;
    skipToPreviousItem: () => Promise<void>;
    seekToTime: (time: number) => Promise<void>;
    nowPlayingItem: any;
    currentPlaybackTime: number;
    currentPlaybackDuration: number;
    isPlaying: boolean;
    playbackState: number;
    addEventListener: (event: string, callback: (event: any) => void) => void;
    removeEventListener: (event: string, callback: (event: any) => void) => void;
    api: {
        library: any;
        music: (endpoint: string, options?: any) => Promise<any>;
    };
}

declare global {
    interface Window {
        MusicKit: {
            configure: (config: any) => Promise<MusicKitInstance>;
            getInstance: () => MusicKitInstance;
            PlaybackStates: {
                none: number;
                loading: number;
                playing: number;
                paused: number;
                stopped: number;
                ended: number;
                seeking: number;
                waiting: number;
                stalled: number;
                completed: number;
            };
        };
    }
}

// Apple Sign In discovery document
const appleDiscovery = {
    authorizationEndpoint: 'https://appleid.apple.com/auth/authorize',
    tokenEndpoint: 'https://appleid.apple.com/auth/token',
};

export const AppleMusicContext = createContext<{
    isConnected: boolean;
    isReady: boolean;
    isInitializing: boolean;
    session: AppleMusicSession | null;
    authenticate: () => Promise<void>;
    disconnect: () => Promise<void>;
    
    getCurrentSong: () => Promise<Song | null>;
    previousTrack: () => void;
    nextTrack: () => void;
    playTrack: () => void;
    pauseTrack: () => void;
    isPlaying: boolean;
    changeTime: (time: number) => void;
    
    // Apple Music specific
    searchSongs: (query: string) => Promise<any[]>;
    playSong: (songId: string) => Promise<void>;
    getRecentlyPlayed: () => Promise<any[]>;
}>({
    isConnected: false,
    isReady: false,
    isInitializing: true,
    session: null,
    authenticate: async () => {},
    disconnect: async () => {},
    getCurrentSong: async () => null,
    previousTrack: () => {},
    nextTrack: () => {},
    playTrack: () => {},
    pauseTrack: () => {},
    isPlaying: false,
    changeTime: (time: number) => {},
    searchSongs: async () => [],
    playSong: async () => {},
    getRecentlyPlayed: async () => [],
});

export const AppleMusicProvider = ({ children }: { children: React.ReactNode }) => {
    const [isConnected, setIsConnected] = useState(false);
    const [isReady, setIsReady] = useState(false);
    const [isInitializing, setIsInitializing] = useState(true);
    const [session, setSession] = useState<AppleMusicSession | null>(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const musicKitRef = useRef<MusicKitInstance | null>(null);
    const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
    const initPromiseRef = useRef<Promise<MusicKitInstance | null> | null>(null);

    // Native OAuth using expo-auth-session
    const redirectUri = makeRedirectUri({
        scheme: 'clusterapp',
    });

    const [request, response, promptAsync] = useAuthRequest(
        {
            responseType: ResponseType.Code,
            clientId: APPLE_SERVICE_ID,
            scopes: ['name', 'email'],
            redirectUri,
            extraParams: {
                response_mode: 'query',
            },
        },
        appleDiscovery
    );

    useEffect(() => {
        initializeAppleMusic();
        
        return () => {
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current);
            }
        };
    }, []);

    useEffect(() => {
        if (response?.type === 'success' && Platform.OS !== 'web') {
            handleNativeAuthSuccess(response.params);
        }
    }, [response]);

    const initializeAppleMusic = async () => {
        if (Platform.OS === 'web') {
            await initializeMusicKitWeb();
        } else {
            if (MUSICKIT_DEVELOPER_TOKEN) {
                setIsReady(true);
            }
            setIsInitializing(false);
        }
    };

    const handleNativeAuthSuccess = async (params: any) => {
        try {
            console.log('Apple Auth Success:', params);
    
            if (params.code) {
                setSession({
                    musicUserToken: params.code,
                    isAuthorized: true,
                });
                setIsConnected(true);
            }
        } catch (error) {
            console.error('Failed to handle native auth:', error);
        }
    };

    const initializeMusicKitWeb = useCallback(async (): Promise<MusicKitInstance | null> => {
        if (Platform.OS !== 'web' || typeof window === 'undefined' || typeof document === 'undefined') {
            setIsInitializing(false);
            return null;
        }

        if (initPromiseRef.current) {
            return initPromiseRef.current;
        }

        if (musicKitRef.current) {
            return musicKitRef.current;
        }

        initPromiseRef.current = new Promise((resolve) => {
            if (window.MusicKit) {
                configureMusicKit().then(resolve);
                return;
            }

            const existingScript = document.querySelector('script[src*="musickit"]');
            if (existingScript) {
                const checkReady = setInterval(() => {
                    if (window.MusicKit) {
                        clearInterval(checkReady);
                        configureMusicKit().then(resolve);
                    }
                }, 100);
                return;
            }

            const script = document.createElement('script');
            script.src = 'https://js-cdn.music.apple.com/musickit/v3/musickit.js';
            script.async = true;
            script.crossOrigin = 'anonymous';
            
            script.onload = () => {
                configureMusicKit().then(resolve);
            };

            script.onerror = () => {
                console.error('Failed to load MusicKit JS SDK');
                setIsInitializing(false);
                setIsReady(false);
                resolve(null);
            };

            document.head.appendChild(script);
        });

        return initPromiseRef.current;
    }, []);

    const configureMusicKit = async (): Promise<MusicKitInstance | null> => {
        try {
            if (!MUSICKIT_DEVELOPER_TOKEN) {
                console.error('MusicKit Developer Token not configured');
                setIsInitializing(false);
                setIsReady(false);
                return null;
            }

            const music = await window.MusicKit.configure({
                developerToken: MUSICKIT_DEVELOPER_TOKEN,
                app: {
                    name: MUSICKIT_APP_NAME,
                    build: MUSICKIT_APP_BUILD,
                },
            });

            musicKitRef.current = music;
            setIsReady(true);
            setIsInitializing(false);

            if (music.isAuthorized) {
                setSession({
                    musicUserToken: music.musicUserToken,
                    isAuthorized: true,
                });
                setIsConnected(true);
            }

            music.addEventListener('playbackStateDidChange', handlePlaybackStateChange);
            music.addEventListener('authorizationStatusDidChange', handleAuthStatusChange);

            console.log('MusicKit initialized successfully');
            return music;

        } catch (error) {
            console.error('Failed to initialize MusicKit:', error);
            setIsInitializing(false);
            setIsReady(false);
            return null;
        }
    };

    const handlePlaybackStateChange = (event: any) => {
        const music = musicKitRef.current;
        if (!music || Platform.OS !== 'web') return;

        const { PlaybackStates } = window.MusicKit;
        setIsPlaying(event.state === PlaybackStates.playing);
    };

    const handleAuthStatusChange = (event: any) => {
        const music = musicKitRef.current;
        if (!music) return;

        if (music.isAuthorized) {
            setSession({
                musicUserToken: music.musicUserToken,
                isAuthorized: true,
            });
            setIsConnected(true);
        } else {
            setSession(null);
            setIsConnected(false);
        }
    };

    const waitForMusicKit = async (): Promise<MusicKitInstance | null> => {
        if (musicKitRef.current) {
            return musicKitRef.current;
        }

        if (initPromiseRef.current) {
            return initPromiseRef.current;
        }

        return initializeMusicKitWeb();
    };

    const authenticate = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = await waitForMusicKit();
                
                if (!music) {
                    console.error('MusicKit not initialized - please check your developer token');
                    return;
                }

                const musicUserToken = await music.authorize();
                
                setSession({
                    musicUserToken,
                    isAuthorized: true,
                });
                setIsConnected(true);
            } else if (Platform.OS === 'ios') {
                try {
                    const credential = await AppleAuthentication.signInAsync({
                        requestedScopes: [
                            AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
                            AppleAuthentication.AppleAuthenticationScope.EMAIL,
                        ],
                    });
                    
                    if (credential.authorizationCode) {
                        setSession({
                            musicUserToken: credential.authorizationCode,
                            isAuthorized: true,
                        });
                        setIsConnected(true);
                        console.log('Apple Sign In successful');
                    }
                } catch (e: any) {
                    if (e.code === 'ERR_REQUEST_CANCELED') {
                        console.log('User canceled Apple Sign In');
                    } else {
                        console.error('Apple Sign In failed:', e);
                    }
                }
            } else {
                if (!APPLE_SERVICE_ID) {
                    console.error('Apple Service ID not configured for Android');
                    return;
                }
                await promptAsync();
            }
        } catch (error) {
            console.error('Failed to authenticate with Apple Music:', error);
        }
    };

    const disconnect = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    await music.unauthorize();
                }
            }
            
            setSession(null);
            setIsConnected(false);
            
        } catch (error) {
            console.error('Failed to disconnect from Apple Music:', error);
        }
    };
    const callAppleMusicAPI = async (endpoint: string, method: string = 'GET') => {
        if (!session?.musicUserToken || !MUSICKIT_DEVELOPER_TOKEN) {
            throw new Error('Not authenticated');
        }

        const response = await fetch(`${APPLE_MUSIC_API_BASE}${endpoint}`, {
            method,
            headers: {
                'Authorization': `Bearer ${MUSICKIT_DEVELOPER_TOKEN}`,
                'Music-User-Token': session.musicUserToken,
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`Apple Music API error: ${response.status}`);
        }

        return response.json();
    };

    const getCurrentSong = async (): Promise<Song | null> => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                
                if (!music || !music.nowPlayingItem) {
                    return null;
                }

                const track = music.nowPlayingItem;
                const attributes = track.attributes;

                return {
                    id: track.id,
                    title: attributes.name,
                    artist: attributes.artistName,
                    album: attributes.albumName,
                    albumArt: attributes.artwork?.url
                        ?.replace('{w}', '300')
                        ?.replace('{h}', '300') || '',
                    duration: attributes.durationInMillis / 1000,
                    currentTime: music.currentPlaybackTime,
                    uri: track.id,
                };
            } else {
                const data = await callAppleMusicAPI('/v1/me/recent/played/tracks?limit=1');
                if (data?.data?.[0]) {
                    const track = data.data[0];
                    const attributes = track.attributes;
                    return {
                        id: track.id,
                        title: attributes.name,
                        artist: attributes.artistName,
                        album: attributes.albumName,
                        albumArt: attributes.artwork?.url
                            ?.replace('{w}', '300')
                            ?.replace('{h}', '300') || '',
                        duration: attributes.durationInMillis / 1000,
                        currentTime: 0,
                        uri: track.id,
                    };
                }
                return null;
            }
        } catch (error) {
            console.error('Failed to get current song:', error);
            return null;
        }
    };

    const previousTrack = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    await music.skipToPreviousItem();
                }
            }
        } catch (error) {
            console.error('Failed to skip to previous track:', error);
        }
    };

    const nextTrack = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    await music.skipToNextItem();
                }
            }
        } catch (error) {
            console.error('Failed to skip to next track:', error);
        }
    };

    const playTrack = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    await music.play();
                    setIsPlaying(true);
                }
            }
        } catch (error) {
            console.error('Failed to play track:', error);
        }
    };

    const pauseTrack = async () => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    music.pause();
                    setIsPlaying(false);
                }
            }
        } catch (error) {
            console.error('Failed to pause track:', error);
        }
    };

    const changeTime = async (positionSeconds: number) => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (music) {
                    await music.seekToTime(positionSeconds);
                }
            }
        } catch (error) {
            console.error('Failed to seek:', error);
        }
    };

    const searchSongs = async (query: string): Promise<any[]> => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (!music) return [];

                const response = await music.api.music(`/v1/catalog/us/search`, {
                    term: query,
                    types: 'songs',
                    limit: 25,
                });

                return response.data.results.songs?.data || [];
            } else {
                const data = await callAppleMusicAPI(`/v1/catalog/us/search?term=${encodeURIComponent(query)}&types=songs&limit=25`);
                return data?.results?.songs?.data || [];
            }
        } catch (error) {
            console.error('Failed to search songs:', error);
            return [];
        }
    };

    const playSong = async (songId: string) => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (!music) return;

                await music.api.music(`/v1/me/player/queue`, {
                    method: 'PUT',
                    body: JSON.stringify({
                        song: songId,
                    }),
                });
                await music.play();
                setIsPlaying(true);
            }
        } catch (error) {
            console.error('Failed to play song:', error);
        }
    };

    const getRecentlyPlayed = async (): Promise<any[]> => {
        try {
            if (Platform.OS === 'web') {
                const music = musicKitRef.current;
                if (!music) return [];

                const response = await music.api.music('/v1/me/recent/played/tracks', {
                    limit: 10,
                });

                return response.data.data || [];
            } else {
                const data = await callAppleMusicAPI('/v1/me/recent/played/tracks?limit=10');
                return data?.data || [];
            }
        } catch (error) {
            console.error('Failed to get recently played:', error);
            return [];
        }
    };

    return (
        <AppleMusicContext.Provider 
            value={{
                isConnected,
                isReady,
                isInitializing,
                session, 
                authenticate,
                disconnect,
                getCurrentSong, 
                previousTrack, 
                nextTrack, 
                playTrack, 
                pauseTrack, 
                isPlaying, 
                changeTime,
                searchSongs,
                playSong,
                getRecentlyPlayed,
            }}
        >
            {children}
        </AppleMusicContext.Provider>
    );
};

export const useAppleMusic = () => {
    const context = useContext(AppleMusicContext);
    if (!context) {
        throw new Error('useAppleMusic must be used within AppleMusicProvider');
    }
    return context;
};
