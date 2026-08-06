import React, { createContext, useContext, useState } from 'react'
import { SettingsContext } from '@/context/SettingsContext';

export type Song = {
    id?: string;
    title: string;
    artist: string;
    album: string;
    duration: number;
    albumArt?: string;
    currentTime?: number;
    uri?: string;
}


export const SpotifyProvider = ({ children }: { children: React.ReactNode }) => {
    const [isConnected, setIsConnected] = useState(false);
    const [token, setToken] = useState<string | null>(null);

    const authenticate = async () => {
    }
}

const AppleMusicContext = createContext<{}>

export const AppleMusicProvider = ({ children }: { children: React.ReactNode }) => {
}

const PlayerContext = createContext<{
    history: Song[];
    getCurrentSong: () => Promise<Song | null>;
    previousTrack: () => void;
    nextTrack: () => void;
    playTrack: () => void;
    pauseTrack: () => void;
    isPlaying: boolean;
    changeTime: (time: number) => void;
}>({
    history: [],
    getCurrentSong: async () => null,
    previousTrack: () => {},
    nextTrack: () => {},
    playTrack: () => {},
    pauseTrack: () => {},
    isPlaying: false,
    changeTime: (time: number) => {},
})

export const PlayerProvider = ({ children }: { children: React.ReactNode }) => {

    const {player} = useContext(SettingsContext);

    if (player.value.defaultService === "spotify") {
        
    } else if (player.value.defaultService === "appleMusic") {
    }

    const [history, setHistory] = useState<Song[]>([]);
    const [currentSong, setCurrentSong] = useState<Song | null>(null);
    const [isPlaying, setIsPlaying] = useState(false);

    const previousTrack = () => {
    }

    const nextTrack = () => {
    }

    const playTrack = () => {
    }

    const pauseTrack = () => {
    }

    const changeTime = (time: number) => {

    }

    return <PlayerContext.Provider value={{ history, currentSong, previousTrack, nextTrack, playTrack, pauseTrack, isPlaying, changeTime }}>{children}</PlayerContext.Provider>
}

