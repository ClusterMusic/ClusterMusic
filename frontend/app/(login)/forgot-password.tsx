import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Image, KeyboardAvoidingView, StyleSheet, Text, TextInput, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

// MISSING ROUTE - forgotPassword(email/username) for password reset


export default function ForgotPasswordScreen() {
  const [identifier, setIdentifier] = useState('');
  const [sent, setSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const disabled = useMemo(() => isLoading || identifier.trim().length === 0, [identifier, isLoading]);

  const onSend = async () => {
    if (disabled) return;
    
    setIsLoading(true);
    try {
      // Implement forgotPassword 

      setSent(true);
    } catch (e) {
      // IMPLEMENT ERROR HANDLING
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView style={containerStyles.container}>
      <KeyboardAvoidingView behavior="padding" style={styles.inner}>

        <Image
          source={require('@/assets/images/fullivory.png')}
          resizeMode="contain"
          style={styles.logo}
        />

        <TextInput
          style={[objectStyles.textBox, styles.input]}
          placeholder="Username or email"
          placeholderTextColor="#777"
          autoCapitalize="none"
          autoCorrect={false}
          value={identifier}
          onChangeText={setIdentifier}
          keyboardType="email-address"
          returnKeyType="done"
          onSubmitEditing={onSend}
        />

        <TouchableOpacity
          style={[styles.button, (disabled || sent) && styles.buttonDisabled]}
          onPress={onSend}
          disabled={disabled || sent}
          activeOpacity={0.8}
        >
          <Text style={[objectStyles.tabText, objectStyles.TextActive]}>
            {sent ? 'Link sent!' : 'Send reset link'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.linksRow} onPress={() => router.push('/(login)/login')}>
          <Text style={objectStyles.lowerOpacity}>Remember your password? Log in</Text>
        </TouchableOpacity>

      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  inner: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    gap: 12,
  },
  logo: {
    width: 180,
    height: 70,
    alignSelf: 'center',
    marginBottom: 16,
  },
  input: {
    flex: 0,
    paddingVertical: 14,
  },
  button: {
    backgroundColor: clusterColors.clusterYellow,
    borderRadius: 9,
    paddingVertical: 12,
    alignItems: 'center',
    marginTop: 4,
  },
  buttonDisabled: {
    opacity: 0.35,
  },
  linksRow: {
    alignItems: 'center',
    marginTop: 4,
  },
});
