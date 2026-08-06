import React from 'react';
import { Pressable, StyleProp, StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useTheme } from '@/hooks/use-theme-color';
import { Fonts, NamedColors } from '@/constants/theme';

type ButtonProps = {
  title: string;
  onPress?: () => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
  loading?: boolean;
  leftIcon?: React.ReactNode;
};

export default function Button({ title, onPress, disabled, style, textStyle, loading, leftIcon }: ButtonProps) {
  const currentTheme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.default,
        styles[currentTheme],
        disabled ? styles.disabled : undefined,
        pressed && !disabled ? styles.pressed : undefined,
        style,
      ]}
    >
      <View style={styles.contentRow}>
        {leftIcon ? <View style={styles.iconWrap}>{leftIcon}</View> : null}
        <Text style={[styles.text, textStyle]}>{loading ? 'Please wait…' : title}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  light: {
    backgroundColor: NamedColors.cyan,
  },
  dark: {
    backgroundColor: NamedColors.cyan,
  },
  default: {
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 14,
  },
  pressed: {
    opacity: 0.9,
  },
  disabled: {
    backgroundColor: NamedColors.cyanDisabled,
  },
  contentRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  iconWrap: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    color: '#fff',
    fontWeight: '600',
    fontFamily: Fonts.sans,
  },
});


