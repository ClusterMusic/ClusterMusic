
import { SplashScreenController } from '@/components/splash';
import { Providers } from '@/context/AppProvider';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import * as Font from 'expo-font';
import { LinearGradient } from 'expo-linear-gradient';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import 'react-native-reanimated';

export const unstable_settings = {
  anchor: '(login)',
};

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const [fontsLoaded, setFontsLoaded] = useState(false);

  const loadFonts = async () => {
    await Font.loadAsync({
      'FuturaPT-Bold': require('../assets/fonts/FuturaCyrillicBold.ttf'),
      'FuturaPT-Book': require('../assets/fonts/FuturaCyrillicBook.ttf'),
      'FuturaPT-Demi': require('../assets/fonts/FuturaCyrillicDemi.ttf'),
      'FuturaPT-EB': require('../assets/fonts/FuturaCyrillicExtraBold.ttf'),
      'FuturaPT-Heavy': require('../assets/fonts/FuturaCyrillicHeavy.ttf'),
      'FuturaPT-Light': require('../assets/fonts/FuturaCyrillicLight.ttf'),
      'FuturaPT-Med': require('../assets/fonts/FuturaCyrillicMedium.ttf'),
    });
    setFontsLoaded(true);
  };

  useEffect(() => {
    loadFonts();
  }, []);

  if (!fontsLoaded) {
    return <View />;
  }


  const CustomDarkTheme = {
    ...DarkTheme,
    colors: {
      ...DarkTheme.colors,
      background: 'transparent',
    },
  };

  const CustomLightTheme = {
    ...DefaultTheme,
    colors: {
      ...DefaultTheme.colors,
      background: 'transparent',
    },
  };

  return (
    <Providers>
      <LinearGradient
        colors={[
          '#252627', 
          '#0F1010'
        ]}
        style={StyleSheet.absoluteFill}
      />
      <ThemeProvider value={colorScheme === 'dark' ? CustomDarkTheme : CustomLightTheme}>
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: {
              backgroundColor: 'transparent',
            },
          }}
        >
          {/*<Stack.Screen name="(login)" options={{ headerShown: false }} />*/}
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        </Stack>
        <StatusBar style="auto" />
        <SplashScreenController />
      </ThemeProvider>
    </Providers>
  );
}
