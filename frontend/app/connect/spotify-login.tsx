import React from 'react';
import { View, StyleSheet, Pressable, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useSpotify } from '@/hooks/spotify';

export default function SpotifyLoginScreen() {
    const router = useRouter();
    const { isConnected, authenticate, session } = useSpotify();
    const [isLoading, setIsLoading] = React.useState(false);

    const handleConnect = async () => {
        setIsLoading(true);
        try {
            await authenticate();
        } catch (error) {
            console.error('Spotify connection failed:', error);
        } finally {
            setIsLoading(false);
        }
    };

    React.useEffect(() => {
        if (isConnected) {
            router.back();
        }
    }, [isConnected]);

    return (
        <ThemedView style={styles.container}>
            <View style={styles.content}>
                <View style={styles.logoContainer}>
                    <View style={styles.spotifyLogo}>
                        <SpotifyIcon />
                    </View>
                </View>

                <ThemedText style={styles.title}>
                    {isConnected ? 'Connected to Spotify' : 'Connect Spotify'}
                </ThemedText>

                <ThemedText style={styles.description}>
                    {isConnected 
                        ? 'Your Spotify account is connected. You can now play music and share your listening activity.'
                        : 'Connect your Spotify account to play music, see what your friends are listening to, and share your favorite tracks.'
                    }
                </ThemedText>

                {isConnected ? (
                    <View style={styles.connectedContainer}>
                        <View style={styles.statusBadge}>
                            <View style={styles.statusDot} />
                            <ThemedText style={styles.statusText}>Connected</ThemedText>
                        </View>
                    </View>
                ) : (
                    <Pressable 
                        style={[styles.button, styles.connectButton]}
                        onPress={handleConnect}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <>
                                <SpotifyIconSmall />
                                <ThemedText style={styles.buttonText}>
                                    Connect with Spotify
                                </ThemedText>
                            </>
                        )}
                    </Pressable>
                )}
            </View>

            <Pressable style={styles.backButton} onPress={() => router.back()}>
                <ThemedText style={styles.backButtonText}>← Back</ThemedText>
            </Pressable>
        </ThemedView>
    );
}

function FeatureItem({ icon, title, description }: { icon: string; title: string; description: string }) {
    return (
        <View style={styles.featureItem}>
            <ThemedText style={styles.featureIcon}>{icon}</ThemedText>
            <View style={styles.featureText}>
                <ThemedText style={styles.featureTitle}>{title}</ThemedText>
                <ThemedText style={styles.featureDescription}>{description}</ThemedText>
            </View>
        </View>
    );
}

function SpotifyIcon() {
    return (
        <View style={styles.spotifyIconContainer}>
            <View style={styles.spotifyBar1} />
            <View style={styles.spotifyBar2} />
            <View style={styles.spotifyBar3} />
        </View>
    );
}

function SpotifyIconSmall() {
    return (
        <View style={styles.spotifyIconSmallContainer}>
            <View style={styles.spotifyBarSmall1} />
            <View style={styles.spotifyBarSmall2} />
            <View style={styles.spotifyBarSmall3} />
        </View>
    );
}
// style AI generated
const styles = StyleSheet.create({
    container: {
        flex: 1,
        padding: 24,
    },
    content: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    logoContainer: {
        marginBottom: 32,
    },
    spotifyLogo: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: '#1DB954',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#1DB954',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.4,
        shadowRadius: 16,
        elevation: 8,
    },
    spotifyIconContainer: {
        width: 50,
        height: 40,
        justifyContent: 'center',
        alignItems: 'center',
    },
    spotifyBar1: {
        position: 'absolute',
        width: 40,
        height: 6,
        backgroundColor: '#000',
        borderRadius: 3,
        top: 5,
        transform: [{ rotate: '-10deg' }],
    },
    spotifyBar2: {
        position: 'absolute',
        width: 34,
        height: 6,
        backgroundColor: '#000',
        borderRadius: 3,
        top: 16,
        transform: [{ rotate: '-10deg' }],
    },
    spotifyBar3: {
        position: 'absolute',
        width: 26,
        height: 6,
        backgroundColor: '#000',
        borderRadius: 3,
        top: 27,
        transform: [{ rotate: '-10deg' }],
    },
    spotifyIconSmallContainer: {
        width: 24,
        height: 20,
        justifyContent: 'center',
        alignItems: 'center',
    },
    spotifyBarSmall1: {
        position: 'absolute',
        width: 20,
        height: 3,
        backgroundColor: '#fff',
        borderRadius: 1.5,
        top: 2,
        transform: [{ rotate: '-10deg' }],
    },
    spotifyBarSmall2: {
        position: 'absolute',
        width: 16,
        height: 3,
        backgroundColor: '#fff',
        borderRadius: 1.5,
        top: 8,
        transform: [{ rotate: '-10deg' }],
    },
    spotifyBarSmall3: {
        position: 'absolute',
        width: 12,
        height: 3,
        backgroundColor: '#fff',
        borderRadius: 1.5,
        top: 14,
        transform: [{ rotate: '-10deg' }],
    },
    title: {
        fontSize: 28,
        fontWeight: '700',
        marginBottom: 12,
        textAlign: 'center',
    },
    description: {
        fontSize: 16,
        opacity: 0.7,
        textAlign: 'center',
        marginBottom: 32,
        lineHeight: 24,
        maxWidth: 320,
    },
    button: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 16,
        paddingHorizontal: 32,
        borderRadius: 30,
        width: '100%',
        maxWidth: 320,
        gap: 10,
    },
    connectButton: {
        backgroundColor: '#1DB954',
    },
    buttonText: {
        fontSize: 17,
        fontWeight: '600',
        color: '#fff',
    },
    connectedContainer: {
        alignItems: 'center',
        gap: 20,
        width: '100%',
    },
    statusBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(29, 185, 84, 0.15)',
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 20,
        gap: 8,
    },
    statusDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: '#1DB954',
    },
    statusText: {
        fontSize: 14,
        color: '#1DB954',
        fontWeight: '600',
    },
    features: {
        marginTop: 48,
        width: '100%',
        maxWidth: 360,
        gap: 16,
    },
    featureItem: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        gap: 16,
        padding: 16,
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        borderRadius: 12,
    },
    featureIcon: {
        fontSize: 24,
    },
    featureText: {
        flex: 1,
    },
    featureTitle: {
        fontSize: 16,
        fontWeight: '600',
        marginBottom: 4,
    },
    featureDescription: {
        fontSize: 14,
        opacity: 0.6,
    },
    backButton: {
        paddingVertical: 12,
    },
    backButtonText: {
        fontSize: 16,
        opacity: 0.7,
    },
});

