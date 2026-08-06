import { useSession } from '@/components/auth';
import * as Routes from '@/components/routes';
import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function CreatePost() {
  const [songId, setSongId] = useState<number | null>(null);
  const [songName, setSongName] = useState('');
  const [caption, setCaption] = useState('');
  const [promotedClique, setPromotedClique] = useState('');
  const [promotedCliqueId, setPromotedCliqueId] = useState<number | null>(null);
  const [selectedCluster, setSelectedCluster] = useState('');
  const [selectedClusterId, setSelectedClusterId] = useState<number | null>(null);
  const [showClusterDropdown, setShowClusterDropdown] = useState(false);
  const [showCliqueDropdown, setShowCliqueDropdown] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const session = useSession();

  const [availableCliques, setAvailableCliques] = useState<{ id: number; name: string }[]>([]);
  const [availableClusters, setAvailableClusters] = useState<{ id: number; title: string }[]>([]);


  const fetchData = useCallback(async () => {
    try {
      setIsLoading(true);
      
      // Fetch cliques user is a member of
      const cliquesResult = await Routes.getCliques(session, { 
        member: session.userId,
        format: 'blob' 
      });
      
      if (cliquesResult.ok) {
        const cliquesData = cliquesResult.data as Routes.CliqueBlob[];
        setAvailableCliques(cliquesData.map(clique => ({
          id: clique.id,
          name: clique.name,
        })));
      }
      
      // Fetch clusters user follows
      const clustersResult = await Routes.getClusters(session, { 
        followed: session.userId,
        format: 'profile' 
      });
      
      if (clustersResult.ok) {
        const clustersData = clustersResult.data as Routes.ClusterProfile[];
        setAvailableClusters(clustersData.map(cluster => ({
          id: cluster.id,
          title: cluster.title,
        })));
      }
    } catch (e) {
      console.error('Failed to fetch data:', e);
    } finally {
      setIsLoading(false);
    }
  }, [session]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handlePost = async () => {
    if (!caption.trim()) {
      Alert.alert('Error', 'Please add a caption');
      return;
    }
    
    if (!promotedCliqueId) {
      Alert.alert('Error', 'Please select a clique');
      return;
    }
    
    if (!selectedClusterId) {
      Alert.alert('Error', 'Please select a cluster');
      return;
    }
    
    // Note: Song selection would need a song search feature
    // For now, we'll use songId = 1 as placeholder
    const finalSongId = songId || 1;
    
    setIsSubmitting(true);
    
    try {
      const result = await Routes.createPost(session, {
        caption: caption.trim(),
        songId: finalSongId,
        cliqueId: promotedCliqueId,
        clusterId: selectedClusterId,
      });
      
      if (result.ok) {
        Alert.alert('Success', 'Post created successfully!', [
          { text: 'OK', onPress: () => router.back() }
        ]);
      } else {
        Alert.alert('Error', result.error);
      }
    } catch (e) {
      Alert.alert('Error', (e as Error).message || 'Failed to create post');
    } finally {
      setIsSubmitting(false);
    }
  };


  return (
    <SafeAreaView style={containerStyles.container}>
      <Text style={objectStyles.title}>Create Post</Text>

      <View style={containerStyles.innerBoxWrapper}>
        <View style={containerStyles.innerBox}>
          <ScrollView 
            style={styles.scrollView}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            <View style={styles.formContainer}>
              <View style={styles.fieldGroup}>
                <Text style={styles.label}>Song (search coming soon):</Text>
                <TextInput
                  style={styles.input}
                  value={songName}
                  onChangeText={setSongName}
                  placeholder="Enter song name..."
                  placeholderTextColor="#777"
                />
              </View>

              <View style={styles.fieldGroup}>
                <Text style={styles.label}>Clique:</Text>
                <TouchableOpacity 
                  style={styles.dropdown}
                  onPress={() => {
                    setShowCliqueDropdown(!showCliqueDropdown);
                    setShowClusterDropdown(false);
                  }}
                  disabled={isLoading}
                >
                  <Text style={[styles.dropdownText, !promotedClique && styles.dropdownPlaceholder]}>
                    {isLoading ? 'Loading cliques...' : (promotedClique || 'Select a clique')}
                  </Text>
                  <Text style={styles.dropdownArrow}>{showCliqueDropdown ? '▲' : '▼'}</Text>
                </TouchableOpacity>
                
                {showCliqueDropdown && !isLoading && (
                  <ScrollView style={styles.dropdownMenu} nestedScrollEnabled>
                    {availableCliques.length === 0 ? (
                      <View style={styles.dropdownItem}>
                        <Text style={styles.dropdownPlaceholder}>No cliques available. Join a clique first!</Text>
                      </View>
                    ) : (
                      availableCliques.map((clique, index) => (
                        <TouchableOpacity 
                          key={clique.id}
                          style={[
                            styles.dropdownItem,
                            index === availableCliques.length - 1 && styles.dropdownItemLast
                          ]}
                          onPress={() => {
                            setPromotedClique(clique.name);
                            setPromotedCliqueId(clique.id);
                            setShowCliqueDropdown(false);
                          }}
                        >
                          <Text style={[
                            styles.dropdownItemText,
                            promotedClique === clique.name && styles.dropdownItemTextActive
                          ]}>{clique.name}</Text>
                        </TouchableOpacity>
                      ))
                    )}
                  </ScrollView>
                )}
              </View>

              <View style={styles.fieldGroup}>
                <Text style={styles.label}>Cluster:</Text>
                <TouchableOpacity 
                  style={styles.dropdown}
                  onPress={() => {
                    setShowClusterDropdown(!showClusterDropdown);
                    setShowCliqueDropdown(false);
                  }}
                  disabled={isLoading}
                >
                  <Text style={[styles.dropdownText, !selectedCluster && styles.dropdownPlaceholder]}>
                    {isLoading ? 'Loading clusters...' : (selectedCluster || 'Select a cluster')}
                  </Text>
                  <Text style={styles.dropdownArrow}>{showClusterDropdown ? '▲' : '▼'}</Text>
                </TouchableOpacity>
                
                {showClusterDropdown && !isLoading && (
                  <ScrollView style={styles.dropdownMenu} nestedScrollEnabled>
                    {availableClusters.length === 0 ? (
                      <View style={styles.dropdownItem}>
                        <Text style={styles.dropdownPlaceholder}>No clusters available. Follow a cluster first!</Text>
                      </View>
                    ) : (
                      availableClusters.map((cluster, index) => (
                        <TouchableOpacity 
                          key={cluster.id}
                          style={[
                            styles.dropdownItem,
                            index === availableClusters.length - 1 && styles.dropdownItemLast
                          ]}
                          onPress={() => {
                            setSelectedCluster(cluster.title);
                            setSelectedClusterId(cluster.id);
                            setShowClusterDropdown(false);
                          }}
                        >
                          <Text style={[
                            styles.dropdownItemText,
                            selectedCluster === cluster.title && styles.dropdownItemTextActive
                          ]}>{cluster.title}</Text>
                        </TouchableOpacity>
                      ))
                    )}
                  </ScrollView>
                )}
              </View>

              <Text style={styles.label}>Caption:</Text>
              <View style={styles.descriptionBox}>
                <TextInput
                  style={styles.descriptionInput}
                  value={caption}
                  onChangeText={setCaption}
                  placeholder="What's on your mind about this song?"
                  placeholderTextColor="#777"
                  multiline
                  textAlignVertical="top"
                />
              </View>
            </View>
          </ScrollView>

          <View style={styles.buttonContainer}>
            <TouchableOpacity 
              style={[styles.actionButton, objectStyles.tabButtonActive, isSubmitting && styles.buttonDisabled]}
              onPress={handlePost}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <ActivityIndicator size="small" color={clusterColors.fullWhite} />
              ) : (
                <Text style={[objectStyles.tabText, objectStyles.TextActive]}>Post</Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
    width: '100%',
  },
  scrollContent: {
    paddingBottom: 20,
  },
  formContainer: {
    width: '100%',
  },
  fieldGroup: {
    marginBottom: 15,
  },
  label: {
    color: clusterColors.clusterTint,
    fontSize: 14,
    fontFamily: 'FuturaPT-Light',
    marginBottom: 5,
  },
  input: {
    backgroundColor: clusterColors.warmBlack,
    borderRadius: 9,
    paddingVertical: 8,
    paddingHorizontal: 15,
    fontSize: 16,
    fontFamily: 'FuturaPT-Book',
    color: clusterColors.fullWhite,
  },
  descriptionBox: {
    backgroundColor: clusterColors.warmBlack,
    borderRadius: 9,
    minHeight: 100,
    padding: 15,
    marginVertical: 10,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
  },
  descriptionInput: {
    width: '100%',
    color: clusterColors.fullWhite,
    fontSize: 14,
    fontFamily: 'FuturaPT-Book',
    textAlign: 'left',
    minHeight: 70,
  },
  dropdown: {
    backgroundColor: clusterColors.warmBlack,
    borderRadius: 9,
    paddingVertical: 8,
    paddingHorizontal: 15,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  dropdownText: {
    fontSize: 16,
    fontFamily: 'FuturaPT-Book',
    color: clusterColors.fullWhite,
  },
  dropdownPlaceholder: {
    color: '#777',
  },
  dropdownArrow: {
    fontSize: 12,
    color: clusterColors.fullWhite,
  },
  dropdownMenu: {
    backgroundColor: clusterColors.warmBlack,
    borderRadius: 9,
    marginTop: 5,
    maxHeight: 150,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#3a3a3a',
  },
  dropdownItem: {
    paddingVertical: 12,
    paddingHorizontal: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#3a3a3a',
  },
  dropdownItemLast: {
    borderBottomWidth: 0,
  },
  dropdownItemText: {
    fontSize: 15,
    fontFamily: 'FuturaPT-Book',
    color: clusterColors.fullWhite,
  },
  dropdownItemTextActive: {
    fontFamily: 'FuturaPT-Bold',
    color: clusterColors.clusterYellow,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 15,
    gap: 15,
  },
  actionButton: {
    flex: 1,
    borderRadius: 9,
    paddingVertical: 10,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
});
