import { HapticTab } from '@/components/haptic-tab';
import MusicBar from '@/components/music-bar';
import { clusterColors } from '@/constants/style';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { BottomTabBar } from '@react-navigation/bottom-tabs';
import { Tabs } from 'expo-router';
import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
export default function TabLayout() {
  const colorScheme = useColorScheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarActiveTintColor: Colors[colorScheme ?? 'light'].tint,
        tabBarBackground: () => (
          <View style={{ flex: 1, backgroundColor: '#086788' }} />
        ),
      }}
      tabBar={(props) => (
        <View style={styles.tabBarContainer}>
            <MusicBar/>
          <BottomTabBar {...props} />
        </View>
      )}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Cluster',
          tabBarIcon: ({ color }) => (<Image
                source={require('@/assets/icons/cluster_logo.png')}
                style={{ width: 28, height: 28, tintColor: color }}
              />),
        }}
      />
      <Tabs.Screen
        name="following"
        options={{
          title: 'Following',
          tabBarIcon: ({ color }) => (<Image
                source={require('@/assets/icons/following_logo.png')}
                style={{ width: 28, height: 28, tintColor: color }}
              />),
        }}
      />
      
      <Tabs.Screen
          name="newPost"
          options={{
            title: '',
            tabBarLabel: () => null,
            tabBarIcon: ({ color }) => (
              <Image
                source={require('@/assets/icons/newPost_logo.png')}
                style={{ width: 40, height: 40, tintColor: clusterColors.clusterYellow }}
              />
            ),
          }}
        />
      <Tabs.Screen
          name="clique"
          options={{
            title: 'Messages',
            tabBarIcon: ({ color }) => (
              <Image
                source={require('@/assets/icons/messages_logo.png')}
                style={{ width: 28, height: 28, tintColor: color }}
              />
            ),
          }}
        />
      <Tabs.Screen
        name="profile/[profileID]"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color }) => (<Image
                source={require('@/assets/icons/profile_logo.png')}
                style={{ width: 28, height: 28, tintColor: color }}
              />),
          href: '/profile/me', 
        }}
      />
      
      
      <Tabs.Screen
        name="settings"
        options={{
          title: 'settings',
          href: null
        }}
      />
      <Tabs.Screen
        name="search"
        options={{
          href: null,
          title: 'Search',
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabBarContainer: {
    backgroundColor: 'transparent',
  },
  playBar: {
    height: 60,
    backgroundColor: '#086788',
    justifyContent: 'center',
    alignItems: 'center',
  },
  playBarText: {
    color: '#FFF4E4',
    fontFamily: 'FuturaPT-Book',
    fontSize: 14,
    fontWeight: '600',
  },
});
