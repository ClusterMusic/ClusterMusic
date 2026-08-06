import { useAuth } from '@/components/auth';
import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import React, { useMemo, useState, } from 'react';
import {
  Image, KeyboardAvoidingView, StyleSheet, Text,
  TextInput,
  TouchableOpacity, View
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function LoginScreen() {
  const { login, isLoading } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const isDisabled = useMemo(
    () => isLoading || username.trim().length === 0 || password.length === 0,
    [isLoading, username, password]
  );

  const onSubmit = async () => {
    if (isDisabled) return;

    setError(null);
    try {
      await login(username.trim(), password);
      router.replace('/(tabs)');
    } catch (e) {
      setError((e as Error).message || 'Login failed');
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
          placeholder="Username"
          placeholderTextColor="#777"
          autoCapitalize="none"
          autoCorrect={false}
          value={username}
          onChangeText={setUsername}
          keyboardType="email-address"
          returnKeyType="next"
        />

        <TextInput
          style={[objectStyles.textBox, styles.input]}
          placeholder="Password"
          placeholderTextColor="#777"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
          returnKeyType="done"
          onSubmitEditing={onSubmit}
        />

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <TouchableOpacity
          style={[styles.button, isDisabled && styles.buttonDisabled]}
          onPress={onSubmit}
          disabled={isDisabled}
          activeOpacity={0.8}
        >
          <Text style={[objectStyles.tabText, objectStyles.TextActive]}>
            {isLoading ? 'Logging in…' : 'Log in'}
          </Text>
        </TouchableOpacity>

        <View style={styles.linksRow}>
          <TouchableOpacity onPress={() => router.push('/(login)/forgot-password')}>
            <Text style={objectStyles.lowerOpacity}>Forgot password?</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => router.push('/(login)/signup')}>
            <Text style={objectStyles.lowerOpacity}>Sign up</Text>
          </TouchableOpacity>
        </View>

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
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  error: {
    color: '#ED4956',
    textAlign: 'center',
    fontFamily: 'FuturaPT-Book',
    fontSize: 13,
  },
});
