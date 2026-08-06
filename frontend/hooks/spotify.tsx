import React, { createContext, useContext, useState, useEffect, useRef } from 'react'
import { Song } from './player';
import * as WebBrowser from 'expo-web-browser';
import { makeRedirectUri, useAuthRequest } from 'expo-auth-session';
import { Platform } from 'react-native';

WebBrowser.maybeCompleteAuthSession();

const SPOTIFY_CLIENT_ID = process.env.EXPO_PUBLIC_SPOTIFY_CLIENT_ID || '';
const SPOTIFY_CLIENT_SECRET = process.env.EXPO_PUBLIC_SPOTIFY_CLIENT_SECRET || '';

const discovery = {
    authorizationEndpoint: 'https://accounts.spotify.com/authorize',
    tokenEndpoint: 'https://accounts.spotify.com/api/token',
};

const scopes = [
    'user-read-email',
    'user-read-private',
    'user-read-playback-state',
    'user-modify-playback-state',
    'user-read-currently-playing',
    'streaming',
    'user-follow-read',
    'playlist-read-private',
    'playlist-read-collaborative',
];

interface SpotifySession {
    accessToken: string;
    refreshToken?: string;
    expiresIn?: number;
}

export const SpotifyContext = createContext<{
    isConnected: boolean;
    isConfigured: boolean;
    session: SpotifySession | null;
    authenticate: () => Promise<void>;
    
    getCurrentSong: () => Promise<Song | null>;
    previousTrack: () => void;
    nextTrack: () => void;
    playTrack: () => void;
    pauseTrack: () => void;
    isPlaying: boolean;
    changeTime: (time: number) => void;
}>({
    isConnected: false,
    isConfigured: false,
    session: null,
    authenticate: async () => {},
    getCurrentSong: async () => null,
    previousTrack: () => {},
    nextTrack: () => {},
    playTrack: () => {},
    pauseTrack: () => {},
    isPlaying: false,
    changeTime: (time: number) => {},
})

export const SpotifyProvider = ({ children }: { children: React.ReactNode }) => {
    const [isConnected, setIsConnected] = useState(false);
    const [session, setSession] = useState<SpotifySession | null>(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [deviceId, setDeviceId] = useState<string | null>(null);
    const playerRef = useRef<any>(null);
    const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
    
    const isConfigured = Boolean(SPOTIFY_CLIENT_ID);

    const [request, response, promptAsync] = useAuthRequest(
        {
            clientId: SPOTIFY_CLIENT_ID,
            scopes: scopes,
            usePKCE: false,
            redirectUri: makeRedirectUri({
                scheme: 'clusterapp'
            }),
        },
        discovery
    );

    console.log('Spotify Redirect URI:', makeRedirectUri({ scheme: 'clusterapp' }));

    useEffect(() => {
        if (response?.type === 'success') {
            const { code } = response.params;
            exchangeCodeForToken(code);
        }
    }, [response]);

    useEffect(() => {
        if (Platform.OS === 'web' && session?.accessToken) {
            initializeWebPlayer();
        }
        return () => {
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current);
            }
        };
    }, [session]);

    const exchangeCodeForToken = async (code: string) => {
        try {
            const credentials = `${SPOTIFY_CLIENT_ID}:${SPOTIFY_CLIENT_SECRET}`;
            const encodedCredentials = btoa(credentials);

            const response = await fetch('https://accounts.spotify.com/api/token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Basic ${encodedCredentials}`,
                },
                body: new URLSearchParams({
                    grant_type: 'authorization_code',
                    code: code,
                    redirect_uri: makeRedirectUri({ scheme: 'clusterapp' }),
                }).toString(),
            });

            const data = await response.json();
            
            if (data.access_token) {
                setSession({
                    accessToken: data.access_token,
                    refreshToken: data.refresh_token,
                    expiresIn: data.expires_in,
                });
                setIsConnected(true);
            }
        } catch (error) {
            console.error('Failed to exchange code for token:', error);
        }
    };

    const initializeWebPlayer = () => {
        if (typeof window === 'undefined') return;

        const script = document.createElement('script');
        script.src = 'https://sdk.scdn.co/spotify-player.js';
        script.async = true;
        document.body.appendChild(script);

        (window as any).onSpotifyWebPlaybackSDKReady = () => {
            const player = new (window as any).Spotify.Player({
                name: 'Cluster Web Player',
                getOAuthToken: (cb: (token: string) => void) => {
                    cb(session!.accessToken);
                },
                volume: 0.5,
            });

            player.addListener('ready', ({ device_id }: { device_id: string }) => {
                console.log('Ready with Device ID', device_id);
                setDeviceId(device_id);
            });

            player.addListener('not_ready', ({ device_id }: { device_id: string }) => {
                console.log('Device ID has gone offline', device_id);
            });

            player.addListener('player_state_changed', (state: any) => {
                if (state) {
                    setIsPlaying(!state.paused);
                }
            });

            player.connect();
            playerRef.current = player;
        };
    };

    const authenticate = async () => {
        try {
            if (!SPOTIFY_CLIENT_ID) {
                console.error('Spotify Client ID not configured. Set EXPO_PUBLIC_SPOTIFY_CLIENT_ID in your environment.');
                return;
            }
            await promptAsync();
        } catch (error) {
            console.error("Failed to authenticate:", error);
        }
    };

    const callSpotifyAPI = async (endpoint: string, method: string = 'GET', body?: any) => {
        if (!session?.accessToken) {
            throw new Error('Not authenticated');
        }

        const response = await fetch(`https://api.spotify.com/v1${endpoint}`, {
            method,
            headers: {
                'Authorization': `Bearer ${session.accessToken}`,
                'Content-Type': 'application/json',
            },
            body: body ? JSON.stringify(body) : undefined,
        });

        if (!response.ok) {
            throw new Error(`Spotify API error: ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    };

    const getCurrentSong = async (): Promise<Song | null> => {
        try {
            const data = await callSpotifyAPI('/me/player/currently-playing');
            
            if (!data || !data.item) {
                return null;
            }

            const track = data.item;
            return {
                id: track.id,
                title: track.name,
                artist: track.artists.map((a: any) => a.name).join(', '),
                album: track.album.name,
                albumArt: track.album.images[0]?.url || '',
                duration: track.duration_ms / 1000,
                currentTime: data.progress_ms / 1000,
                uri: track.uri,
            };
        } catch (error) {
            console.error('Failed to get current song:', error);
            return null;
        }
    };

    const previousTrack = async () => {
        try {
            await callSpotifyAPI('/me/player/previous', 'POST');
        } catch (error) {
            console.error('Failed to skip to previous track:', error);
        }
    };

    const nextTrack = async () => {
        try {
            await callSpotifyAPI('/me/player/next', 'POST');
        } catch (error) {
            console.error('Failed to skip to next track:', error);
        }
    };

    const playTrack = async () => {
        try {
            if (Platform.OS === 'web' && playerRef.current) {
                await playerRef.current.resume();
            } else {
                await callSpotifyAPI('/me/player/play', 'PUT');
            }
            setIsPlaying(true);
        } catch (error) {
            console.error('Failed to play track:', error);
        }
    };

    const pauseTrack = async () => {
        try {
            if (Platform.OS === 'web' && playerRef.current) {
                await playerRef.current.pause();
            } else {
                await callSpotifyAPI('/me/player/pause', 'PUT');
            }
            setIsPlaying(false);
        } catch (error) {
            console.error('Failed to pause track:', error);
        }
    };

    const changeTime = async (positionMs: number) => {
        try {
            if (Platform.OS === 'web' && playerRef.current) {
                await playerRef.current.seek(positionMs);
            } else {
                await callSpotifyAPI(`/me/player/seek?position_ms=${positionMs}`, 'PUT');
            }
        } catch (error) {
            console.error('Failed to seek:', error);
        }
    };

    return (
        <SpotifyContext.Provider 
            value={{
                isConnected,
                isConfigured,
                session, 
                authenticate,
                getCurrentSong, 
                previousTrack, 
                nextTrack, 
                playTrack, 
                pauseTrack, 
                isPlaying, 
                changeTime 
            }}
        >
            {children}
        </SpotifyContext.Provider>
    );
};

export const useSpotify = () => {
    const context = useContext(SpotifyContext);
    if (!context) {
        throw new Error('useSpotify must be used within SpotifyProvider');
    }
    return context;
};