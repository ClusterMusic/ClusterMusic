import React, { ReactNode } from "react";
import { SettingsProvider } from "./SettingsContext";
import { SpotifyProvider } from "@/hooks/spotify";
import { AppleMusicProvider } from "@/hooks/applemusic";
import { AuthProvider } from "@/components/auth";

interface ProvidersProps {
  children: ReactNode;
}

export const Providers: React.FC<ProvidersProps> = ({ children }) => {
  return (
    <SettingsProvider>
      <AuthProvider>
        <SpotifyProvider>
          <AppleMusicProvider>
            {children}
          </AppleMusicProvider>
        </SpotifyProvider>
      </AuthProvider>
    </SettingsProvider>
  );
};
