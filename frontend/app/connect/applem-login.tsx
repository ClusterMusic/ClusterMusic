import React from 'react';
import { View, StyleSheet, Pressable, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useAppleMusic } from '@/hooks/applemusic';

export default function AppleMusicLoginScreen() {
    const router = useRouter();
    const { isConnected, authenticate, disconnect, isInitializing, isReady } = useAppleMusic();
    const [isLoading, setIsLoading] = React.useState(false);

    const handleConnect = async () => {
        setIsLoading(true);
        try {
            await authenticate();
            if (isConnected) {
                router.back();
            }
        } catch (error) {
            console.error('Apple Music connection failed:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleDisconnect = async () => {
        setIsLoading(true);
        try {
            await disconnect();
        } catch (error) {
            console.error('Apple Music disconnect failed:', error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <ThemedView style={styles.container}>
            <View style={styles.content}>
                <View style={styles.logoContainer}>
                    <View style={styles.appleMusicLogo}>
                        <ThemedText style={styles.logoIcon}>♫</ThemedText>
                    </View>
                </View>

                <ThemedText style={styles.title}>
                    {isConnected ? 'Connected to Apple Music' : 'Connect Apple Music'}
                </ThemedText>

                <ThemedText style={styles.description}>
                    {isConnected 
                        ? 'Your Apple Music account is connected. You can now play music!'
                        : 'Connect your Apple Music account to play music and share your favorite tracks.'
                    }
                </ThemedText>

                {isConnected ? (
                    <View style={styles.connectedContainer}>
                        <View style={styles.statusBadge}>
                            <View style={styles.statusDot} />
                            <ThemedText style={styles.statusText}>Connected</ThemedText>
                        </View>

                        <Pressable 
                            style={[styles.button, styles.disconnectButton]}
                            onPress={handleDisconnect}
                            disabled={isLoading}
                        >
                            {isLoading ? (
                                <ActivityIndicator color="#fff" />
                            ) : (
                                <ThemedText style={styles.buttonText}>Disconnect</ThemedText>
                            )}
                        </Pressable>
                    </View>
                ) : (
                    <Pressable 
                        style={[styles.button, styles.connectButton, (isInitializing || isLoading) && styles.buttonDisabled]}
                        onPress={handleConnect}
                        disabled={isLoading || isInitializing}
                    >
                        {isLoading || isInitializing ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <>
                                <ThemedText style={styles.buttonIcon}>♫</ThemedText>
                                <ThemedText style={styles.buttonText}>
                                    Connect with Apple Music
                                </ThemedText>
                            </>
                        )}
                    </Pressable>
                )}
                
                {isInitializing && (
                    <ThemedText style={styles.initializingText}>
                        Initializing Apple Music...
                    </ThemedText>
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
    appleMusicLogo: {
        width: 100,
        height: 100,
        borderRadius: 22,
        backgroundColor: '#FA243C',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#FA243C',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.4,
        shadowRadius: 16,
        elevation: 8,
    },
    logoIcon: {
        fontSize: 48,
        color: '#fff',
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
        borderRadius: 12,
        width: '100%',
        maxWidth: 320,
        gap: 10,
    },
    connectButton: {
        backgroundColor: '#FA243C',
    },
    disconnectButton: {
        backgroundColor: '#666',
    },
    buttonIcon: {
        fontSize: 20,
        color: '#fff',
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
        backgroundColor: 'rgba(76, 217, 100, 0.15)',
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 20,
        gap: 8,
    },
    statusDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: '#4CD964',
    },
    statusText: {
        fontSize: 14,
        color: '#4CD964',
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
    buttonDisabled: {
        opacity: 0.6,
    },
    initializingText: {
        marginTop: 12,
        fontSize: 14,
        opacity: 0.5,
        textAlign: 'center',
    },
    webOnlyContainer: {
        alignItems: 'center',
        gap: 12,
        padding: 24,
        backgroundColor: 'rgba(250, 36, 60, 0.08)',
        borderRadius: 16,
        borderWidth: 1,
        borderColor: 'rgba(250, 36, 60, 0.2)',
    },
    webOnlyBadge: {
        fontSize: 14,
        fontWeight: '700',
        color: '#FA243C',
        backgroundColor: 'rgba(250, 36, 60, 0.15)',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        overflow: 'hidden',
    },
    webOnlyText: {
        fontSize: 14,
        opacity: 0.7,
        textAlign: 'center',
    },
});

