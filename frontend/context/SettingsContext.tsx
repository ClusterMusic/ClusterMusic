import AsyncStorage from "@react-native-async-storage/async-storage";
import * as Location from "expo-location";
import React, {
  createContext,
  Dispatch,
  ReactNode,
  SetStateAction,
  useEffect,
  useState,
} from "react";


//add section here
export interface PrivacySettings {
  profileVisible: boolean;
  showActivityStatus: boolean;
  allowTagging: boolean;
}

export interface NotificationSettings {
  push: boolean;
  mentions: boolean;
  messages: boolean;
  newFollowers: boolean;
}

export interface UISettings {
  language: string;
  fontSize: number;
}

export interface LocationData {
  latitude: number | null;
  longitude: number | null;
  city?: string | null;
  region?: string | null;
}

export interface AccountSettings {
  autoPlay: boolean;
  location: LocationData | null;
  profilePicture: string | null;
  biography: string | null;
  displayName: string;
}

export interface PlayerSettings {
  defaultService: "spotify" | "appleMusic";
}


export interface SettingsSection<T> {
  name: string;
  value: T;
  setValue: Dispatch<SetStateAction<T>>;
}


export interface SettingsContextType {
  privacy: SettingsSection<PrivacySettings>;
  notifications: SettingsSection<NotificationSettings>;
  ui: SettingsSection<UISettings>;
  account: SettingsSection<AccountSettings>;
  player: SettingsSection<PlayerSettings>;
}

export const SettingsContext = createContext<SettingsContextType>(
  {} as SettingsContextType
);

export const SettingsProvider = ({ children }: { children: ReactNode }) => {

  function usePersistentSetting<T>(
    key: string,
    defaultValue: T
  ): [T, Dispatch<SetStateAction<T>>] {
    const [value, setValue] = useState<T>(defaultValue);

    useEffect(() => {
      (async () => {
        try {
          const stored = await AsyncStorage.getItem(key);
          if (stored !== null) setValue(JSON.parse(stored));
        } catch (err) {
          console.warn(`Failed to load '${key}':`, err);
        }
      })();
    }, [key]);

    useEffect(() => {
      (async () => {
        try {
          await AsyncStorage.setItem(key, JSON.stringify(value));
        } catch (err) {
          console.warn(`Failed to save '${key}':`, err);
        }
      })();
    }, [key, value]);

    return [value, setValue];
  }

  //call section here
  const [privacy, setPrivacy] = usePersistentSetting<PrivacySettings>("privacy", {
    profileVisible: false,
    showActivityStatus: true,
    allowTagging: true,
  });

  const [notifications, setNotifications] =
    usePersistentSetting<NotificationSettings>("notifications", {
      push: true,
      mentions: true,
      messages: true,
      newFollowers: true,
    });

  const [ui, setUI] = usePersistentSetting<UISettings>("ui", {
    language: "en",
    fontSize: 16,
  });

  const [account, setAccount] = usePersistentSetting<AccountSettings>("account", {
    autoPlay: true,
    location: null,
    profilePicture: "",
    biography: "",
    displayName: "",
  });

  const [player, setPlayer] = usePersistentSetting<PlayerSettings>("player", {
    defaultService: "spotify",
  });

  //add functionality of section here
  useEffect(() => {
    if (account.location == null) {
      (async () => {
        try {
          const { status } = await Location.requestForegroundPermissionsAsync();
          if (status !== "granted") return;

          const loc = await Location.getCurrentPositionAsync({});
          const address = await Location.reverseGeocodeAsync({
            latitude: loc.coords.latitude,
            longitude: loc.coords.longitude,
          });

          const locationData: LocationData = {
            latitude: loc.coords.latitude,
            longitude: loc.coords.longitude,
            city: address[0]?.city ?? null,
            region: address[0]?.region ?? null,
          };

          setAccount((prev) => ({
            ...prev,
            location: locationData,    
          }));
        } catch (err) {
          console.warn("Failed to fetch location:", err);
        }
      })();
    }
  }, []);


  useEffect(() => {
    if (account.location && (account.location.city == null)) {
      alert("City information is required to continue.");
      throw new Error("Application stopped: city is required.");
    }
  }, [account.location?.city]);

  //call section here
  return (
    <SettingsContext.Provider
      value={{
        account: {
          name: "account",
          value: account,
          setValue: setAccount,
        },
        privacy: {
          name: "privacy",
          value: privacy,
          setValue: setPrivacy,
        },
        notifications: {
          name: "notifications",
          value: notifications,
          setValue: setNotifications,
        },
        player: {
          name: "player",
          value: player,
          setValue: setPlayer,
        },
        ui: {
          name: "ui",
          value: ui,
          setValue: setUI,
        },
      }}
    >
      {children}
    </SettingsContext.Provider>
  );
};
