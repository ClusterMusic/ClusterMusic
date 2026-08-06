import React, { forwardRef } from 'react';
import { StyleProp, StyleSheet, TextInput, TextInputProps, TextStyle } from 'react-native';

import { useTheme } from '@/hooks/use-theme-color';
import { FontNames, Fonts, NamedColors} from '@/constants/theme';

type TextFieldProps = TextInputProps & {
  style?: StyleProp<TextStyle>;
};

 const TextField = forwardRef<TextInput, TextFieldProps>((props, ref) => {
  const { style, placeholderTextColor, ...rest } = props;
  const mytheme = useTheme()

  return (
    <TextInput
      ref={ref}
      placeholderTextColor={placeholderTextColor ?? NamedColors.steel}
      style={[{
        ...styles[mytheme],
        ...styles.default
      }, style]}
      {...rest}
    />
  );
});

TextField.displayName = 'TextField';

export default TextField;

const styles = StyleSheet.create({
  light: {
    backgroundColor: NamedColors.salt,
    borderColor: NamedColors.steel,
    color: '#262626',
  },
  dark: {
    backgroundColor: NamedColors.graphite,
    borderColor: NamedColors.steel,

    color: NamedColors.ivory,
  },
  default: {
    borderRadius: 4,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    lineHeight: 24,
    borderWidth: 1,
    marginBottom: 10,
    fontFamily: Fonts.sans,
  },
});


