import React from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useTheme } from '@/hooks/use-theme-color';
import { NamedColors } from '@/constants/theme';

export default function BackButton() {
  const router = useRouter();
  const theme = useTheme();
  const color = theme === 'dark' ? NamedColors.ivory : NamedColors.graphite;

  return (
    <Pressable accessibilityLabel="Go back" onPress={() => router.back()} style={({ pressed }) => [styles.hit, pressed && styles.pressed]}>
      <IconSymbol name="chevron.left" color={color} size={26} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  hit: {
    padding: 8,
  },
  pressed: {
    opacity: 0.6,
  },
});


