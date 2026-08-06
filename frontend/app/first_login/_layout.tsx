import { Stack } from 'expo-router';

export default function FirstLoginLayout() {
    return (
        <Stack
            screenOptions={{
                headerShown: false,
                animation: 'fade',
            }}
        >
            <Stack.Screen name="connect" />
        </Stack>
    );
}

