import { useSession } from '@/components/auth';
import * as Routes from '@/components/routes';
import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Dimensions, FlatList, Image, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from "react-native-safe-area-context";

interface CliqueItem {
  id: number;
  name: string;
  message?: string;
  type: 'active' | 'regular';
  image?: string;
}

export default function Clique() {
  const [text, setText] = useState<string>('');
  const [selectedTab, setSelectedTab] = useState<'clique' | 'private'>('clique');
  const [cliques, setCliques] = useState<CliqueItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const session = useSession();

  const fetchCliques = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const result = await Routes.getCliques(session, { 
        member: session.userId,
        format: 'profile' 
      });
      
      if (!result.ok) {
        setError(result.error);
        return;
      }
      
      const cliquesData = result.data as Routes.CliqueProfile[];
      
      // Missing Data: lastMessage, lastMessageAuthor, hasUnread fields
      const formattedCliques: CliqueItem[] = cliquesData.map((clique, index) => ({
        id: clique.id,
        name: clique.name,
        message: `${clique.members} members · ${clique.followers} followers`,
        type: index < 3 ? 'active' : 'regular',
        image: clique.image,
      }));
      
      setCliques(formattedCliques);
    } catch (e) {
      setError((e as Error).message || 'Failed to load cliques');
    } finally {
      setIsLoading(false);
    }
  }, [session]);

  useEffect(() => {
    fetchCliques();
  }, [fetchCliques, selectedTab]);


  const filteredData = cliques.filter(clique => 
    clique.name.toLowerCase().includes(text.toLowerCase())
  );
  
  // Missing data - Private messages 
  const data = selectedTab === 'clique' ? filteredData : [];

  const renderItem = ({ item }: { item: CliqueItem }) => {
    

    const handlePress = () => {
      router.replace('/chat/'); // temporary route until chat_id is added
    };

    if (item.type === 'active') {
      return (
        <TouchableOpacity onPress={handlePress} activeOpacity={0.8}>
          <View style={styles.activeBox}>
            <View style={containerStyles.genericRow}>
              <Image
                source={require('@/assets/icons/profile.png')}
                style={styles.icon}
              />
              <View style={styles.textWrapper}>
                <Text style={objectStyles.subsectionTitle}>{item.name}</Text>
                <Text style={styles.boxSubtitle}>{item.message}</Text>
              </View>
            </View>
          </View>
        </TouchableOpacity>
      );
    } else {
      return (
        <TouchableOpacity onPress={handlePress} activeOpacity={0.8}>
          <View style={styles.box}>
            <View style={containerStyles.genericRow}>
              <Image
                source={require('@/assets/icons/profile.png')}
                style={styles.icon}
              />
              <Text style={objectStyles.subsectionTitle}>{item.name}</Text>
            </View>
          </View>
        </TouchableOpacity>
      );
    }
  };

  if (isLoading) {
    return (
      <SafeAreaView style={[containerStyles.container, styles.centerContent]}>
        <ActivityIndicator size="large" color={clusterColors.clusterYellow} />
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={[containerStyles.container, styles.centerContent]}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={fetchCliques}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={containerStyles.container}>
      <Text style={objectStyles.title}>Messages</Text>

      <View style={containerStyles.innerBoxWrapper}>
        <View style={containerStyles.innerBox}>
          <View style={containerStyles.buttonRow}>
            <TouchableOpacity
              style={[
                styles.tabButton,
                selectedTab === 'clique' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setSelectedTab('clique')}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  selectedTab === 'clique' ? objectStyles.TextActive : objectStyles.TextInactive,
                ]}
              >
                My Cliques
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.tabButton,
                selectedTab === 'private' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setSelectedTab('private')}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  selectedTab === 'private' ? objectStyles.TextActive : objectStyles.TextInactive,
                ]}
              >
                Private
              </Text>
            </TouchableOpacity>
          </View>

          <View style={containerStyles.searchRow}>
            <TextInput
              style={objectStyles.textBox}
              value={text}
              onChangeText={setText}
              placeholder="Search ..."
              placeholderTextColor="#777"
            />
            <TouchableOpacity onPress={() => console.log('Search pressed')}>
              <Image
                source={require('@/assets/icons/search.png')}
                style={styles.searchIcon}
              />
            </TouchableOpacity>
          </View>

          <FlatList
            data={data}
            renderItem={renderItem}
            keyExtractor={(item) => item.id.toString()}
            style={styles.flatList}
            showsVerticalScrollIndicator={false}
            ListEmptyComponent={
              <Text style={styles.emptyText}>
                {selectedTab === 'clique' 
                  ? 'No cliques found. Join a clique to see them@' 
                  : 'Private messages coming soon'}
              </Text>
            }
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const { width } = Dimensions.get('window');

const styles = StyleSheet.create({
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    color: '#ED4956',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 16,
    fontFamily: 'FuturaPT-Book',
  },
  retryButton: {
    backgroundColor: clusterColors.clusterYellow,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 9,
  },
  retryButtonText: {
    color: clusterColors.fullWhite,
    fontFamily: 'FuturaPT-Bold',
    fontSize: 14,
  },
  emptyText: {
    color: clusterColors.dimGray,
    fontSize: 16,
    fontFamily: 'FuturaPT-Book',
    textAlign: 'center',
    padding: 20,
  },
  tabButton: {
    flex: 1,
    marginHorizontal: 5,
    borderRadius: 9,
    paddingVertical: 10,
    alignItems: 'center',
  },

  searchIcon: {
    ...objectStyles.genericIcon,
    marginLeft: 10,
  },
  flatList: {
    width: '100%',
    flex: 1,
  },
  box: {
    ...containerStyles.genericRow,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#3a3a3a',
  },
  activeBox: {
    
    flex: 0,  
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#3a3a3a',
    backgroundColor: '#333',
  },

  icon: {
    width: 35,
    height: 35,
    tintColor: '#FFE9C9',
    marginRight: 10,
  },
  textWrapper: {
    flexDirection: 'column',
    flex: 1,
  },
  boxSubtitle: {
    ...objectStyles.lowerOpacity,
    fontSize: 13,
    marginTop: 2,
  },
});
