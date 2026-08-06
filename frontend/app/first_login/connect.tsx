import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { useAppleMusic } from '@/hooks/applemusic';
import { useSpotify } from '@/hooks/spotify';
import { useRouter } from 'expo-router';
import React from 'react';
import { Animated, Dimensions, Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

const { width } = Dimensions.get('window');

export default function ConnectScreen() {
    const router = useRouter();
    const spotify = useSpotify();
    const appleMusic = useAppleMusic();
    const [isAppleLoading, setIsAppleLoading] = React.useState(false);
    const [isSpotifyLoading, setIsSpotifyLoading] = React.useState(false);
    
    const fadeAnim = React.useRef(new Animated.Value(0)).current;
    const slideAnim = React.useRef(new Animated.Value(30)).current;

    React.useEffect(() => {
        Animated.parallel([
            Animated.timing(fadeAnim, {
                toValue: 1,
                duration: 600,
                useNativeDriver: true,
            }),
            Animated.timing(slideAnim, {
                toValue: 0,
                duration: 600,
                useNativeDriver: true,
            }),
        ]).start();
    }, []);

    const handleSpotifyConnect = async () => {
        setIsSpotifyLoading(true);
        try {
            await spotify.authenticate();
        } finally {
            setIsSpotifyLoading(false);
        }
    };

    const handleAppleMusicConnect = async () => {
        setIsAppleLoading(true);
        try {
            await appleMusic.authenticate();
        } finally {
            setIsAppleLoading(false);
        }
    };

    const handleSkip = () => {
        router.replace('/(tabs)');
    };

    const handleContinue = () => {
        router.replace('/(tabs)');
    };

    const isAnyConnected = spotify.isConnected || appleMusic.isConnected;

    return (
        <SafeAreaView style={containerStyles.container}>
            <Animated.View 
                style={[
                    styles.content,
                    {
                        opacity: fadeAnim,
                        transform: [{ translateY: slideAnim }],
                    }
                ]}
            >
                <View style={styles.header}>
                    <Text style={objectStyles.title}>Connect Your Music</Text>
                    <Text style={objectStyles.TextActive}>
                        Link your streaming service
                    </Text>
                </View>

                <View style={styles.servicesContainer}>
                    {/* Spotify Card */}
                    <TouchableOpacity
                        style={[
                            styles.serviceCard,
                            spotify.isConnected && styles.serviceCardConnected,
                            isSpotifyLoading && styles.serviceCardLoading,
                        ]}
                        onPress={handleSpotifyConnect}
                        disabled={spotify.isConnected || isSpotifyLoading}
                    >
                        <Image
                            source={require('@/assets/icons/spotify-square-color-icon.png')}
                            style={styles.MusicIcon}
                        />
                        <View style={styles.serviceInfo}>
                            <Text style={styles.serviceName}>Spotify Music</Text>
                            <Text style={styles.serviceStatus}>
                                {spotify.isConnected 
                                    ? 'Connected' 
                                    : isSpotifyLoading 
                                        ? 'Connecting...' 
                                        : 'Use Spotify Premium for best experience.'}
                            </Text>
                        </View>
                        
                    </TouchableOpacity>

                    <View style={styles.orDivider}>
                        <View style={styles.dividerLine} />
                        <Text style={styles.orText}>or</Text>
                        <View style={styles.dividerLine} />
                    </View>

                    {/* Apple Music Card */}
                    <TouchableOpacity
                        style={[
                            styles.serviceCard,
                            appleMusic.isConnected && styles.serviceCardConnected,
                            (appleMusic.isInitializing || isAppleLoading) && styles.serviceCardLoading,
                        ]}
                        onPress={handleAppleMusicConnect}
                        disabled={appleMusic.isConnected || appleMusic.isInitializing || isAppleLoading}
                    >
                        <Image
                                        source={require('@/assets/icons/apple_logo.png')}
                                        style={styles.MusicIcon}
                        />
                        
                        <View style={styles.serviceInfo}>
                            <Text style={styles.serviceName}>Apple Music</Text>
                            <Text style={styles.serviceStatus}>
                                {appleMusic.isConnected 
                                    ? 'Connected' 
                                    : appleMusic.isInitializing 
                                        ? 'Loading...'
                                        : isAppleLoading
                                            ? 'Connecting...'
                                            : 'Tap to connect. Limited to iOS.'}
                            </Text>
                        </View>
                    </TouchableOpacity>
                </View>

                <View style={styles.footer}>
                    {isAnyConnected ? (
                        <TouchableOpacity style={styles.continueButton} onPress={handleContinue}>
                            <Text style={objectStyles.subsectionTitle}>
                                Continue to Cluster
                            </Text>
                        </TouchableOpacity>
                    ) : (
                        <TouchableOpacity style={styles.skipButton} onPress={handleSkip}>
                            <Text style={objectStyles.subsectionTitle}>
                                Skip for now
                            </Text>
                        </TouchableOpacity>
                    )}

                </View>
            </Animated.View>
        </SafeAreaView>
    );
}

// style AI generated
const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    content: {
        flex: 1,
        padding: 24,
        justifyContent: 'space-between',
    },
    header: {
        alignItems: 'center',
        paddingTop: 60,
    },
    servicesContainer: {
        gap: 16,
    },
    serviceCard: {
        ...containerStyles.genericRow,
        padding: 20,
        borderRadius: 5,
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
    },
    serviceCardConnected: {
        borderColor: 'rgba(76, 217, 100, 0.5)',
        backgroundColor: 'rgba(76, 217, 100, 0.08)',
    },
    serviceCardLoading: {
        opacity: 0.7,
    },
    MusicIcon: {
        width: 48,
        height: 48,
    },
    serviceInfo: {
        flex: 1,
        marginLeft: 16,
    },
    serviceName: {
        ...objectStyles.subsectionTitle,
        alignSelf: 'center',
        fontSize: 18,
        marginBottom: 4,
    },
    serviceStatus: {
        ...objectStyles.lowerOpacity, 
        alignSelf: 'center',
        paddingLeft: 0, 
        fontSize: 14,
        opacity: 0.6,
    },

    checkmarkApple: {
        backgroundColor: '#FA243C',
    },
    checkmarkText: {
        color: '#fff',
        fontSize: 18,
        fontWeight: '700',
    },
    connectArrow: {
        width: 32,
        height: 32,
        justifyContent: 'center',
        alignItems: 'center',
    },
    arrowText: {
        fontSize: 20,
        opacity: 0.5,
    },
    orDivider: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
        tintColor: clusterColors.clusterTint
    },
    dividerLine: {
        flex: 1,
        height: 1,
        backgroundColor: clusterColors.clusterTint,
    },
    orText: {
        ...objectStyles.lowerOpacity,
        color: clusterColors.clusterTint,   
        paddingHorizontal: 16,
        fontSize: 12,
        opacity: 0.5,
        textTransform: 'uppercase',
        letterSpacing: 1,
    },
    footer: {
        alignItems: 'center',
        paddingBottom: 40,
    },
    continueButton: {
        width: '100%',
        paddingVertical: 18,
        borderRadius: 14,
        backgroundColor: '#fff',
        alignItems: 'center',
        marginBottom: 16,
    },
    continueButtonText: {
        fontSize: 17,
        fontWeight: '700',
        color: '#000',
    },
    skipButton: {
        paddingVertical: 16,
        paddingHorizontal: 32,
    },
    skipButtonText: {
        fontSize: 16,
        opacity: 0.6,
    },
    privacyNote: {
        ...objectStyles.lowerOpacity, 
        fontSize: 12,
        opacity: 0.4,
        textAlign: 'center',
        marginTop: 16,
        maxWidth: 280,
        lineHeight: 18,
    },
});

