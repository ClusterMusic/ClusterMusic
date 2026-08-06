import { useAuth } from '@/components/auth';
import * as Routes from '@/components/routes';
import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Image, KeyboardAvoidingView, StyleSheet, Text, TextInput, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
export default function SignUpScreen() {
  const { register, isLoading } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [created, setCreated] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const disabled = useMemo(() => {
    return isLoading || username.trim().length === 0 || email.trim().length === 0 || password.length < 6;
  }, [isLoading, username, email, password]);

  const onCreate = async () => {
    if (disabled) return;
    
    setError(null);
    
    try {
      await register(username.trim(), password, "", 0);
      setCreated(true);
      router.replace('/first_login/connect');
    } catch (e) {
      setError((e as Error).message || 'Registration failed');
    }
    const result = await Routes.register(username.trim(), password, 'my biography', 0);
    if (!result.ok) {
      setCreated(false);
      return;
    }
    setCreated(true);
    router.navigate('/(login)/login');
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
          placeholder="Username"
          placeholderTextColor="#777"
          autoCapitalize="none"
          value={username}
          onChangeText={setUsername}
        />
        <TextInput
          style={[objectStyles.textBox, styles.input]}
          placeholder="Email"
          placeholderTextColor="#777"
          autoCapitalize="none"
          keyboardType="email-address"
          value={email}
          onChangeText={setEmail}
        />
        <TextInput
          style={[objectStyles.textBox, styles.input]}
          placeholder="Password"
          placeholderTextColor="#777"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        <TouchableOpacity
          style={[styles.button, (disabled || created) && styles.buttonDisabled]}
          onPress={onCreate}
          disabled={disabled || created}
          activeOpacity={0.8}
        >
          <Text style={[objectStyles.tabText, objectStyles.TextActive]}>
            {created ? 'Account created!' : 'Sign up'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.linksRow} onPress={() => router.push('/(login)/login')}>
          <Text style={objectStyles.lowerOpacity}>Already have an account? Log in</Text>
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
