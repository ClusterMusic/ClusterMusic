import { useSession } from '@/components/auth';
import * as Routes from '@/components/routes';
import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, Dimensions, FlatList, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

const { width: SCREEN_WIDTH } = Dimensions.get('window');


interface CarouselItem {
  id: string;
  title: string;
  color: string;
}

interface ProfileSection {
  id: string;
  title: string;
  type: 'carousel';
  items: CarouselItem[];
  visibleItems: number;
  visible: boolean;
  order: number;
}

interface LeaderboardEntry {
  id: string;
  rank: number;
  name: string;
  location: string;
  isUser: boolean;
}

interface ScrollRef {
  ref: FlatList | null;
  currentIndex: number;
}

export default function Profile() {

  const { profileID } = useLocalSearchParams(); 
  const session = useSession();
  
  const isMe = useRef(!profileID || String(profileID) === String(session.userId));
  
  const [selectedTab, setSelectedTab] = useState<'profile' | 'achievements'>('profile');
  const [isEditMode, setIsEditMode] = useState<boolean>(false);
  const [following, setFollowing] = useState<boolean>(false);
  const [joining, setJoining] = useState<'Join' | 'Requested' | 'Member'>('Join');
  const [expanded, setExpanded] = useState<boolean>(false);
  const [currentLeaderboard, setCurrentLeaderboard] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const leaderboardScroll = useRef<FlatList>(null);
  const sectionScrollRefs = useRef<Record<string, ScrollRef>>({});

  const [user, setUser] = useState({
    type: "user", // user or clique
    name: "",
    profileImage: "",
    beats: 0,
    followers: 0,
    following: 0,
    bio: "",
    Banner: ""
  });

  const [editBio, setEditBio] = useState(user.bio);
  
  // Fetch user profile data
  const fetchProfile = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const userId = profileID ? Number(profileID) : session.userId;
      const result = await Routes.getUser(session, userId, 'profile');
      
      if (!result.ok) {
        setError(result.error);
        return;
      }
      
      const userData = result.data as Routes.UserProfile;
      setUser({
        type: "user",
        name: userData.username,
        profileImage: userData.image || "https://via.placeholder.com/150",
        beats: 0, // Missing column - beats
        followers: userData.followers,
        following: userData.following,
        bio: userData.biography,
        Banner: "" // Missing column - banner
      });
      setEditBio(userData.biography);
      
      isMe.current = userData.id === session.userId;
      
    } catch (e) {
      setError((e as Error).message || 'Failed to load profile');
    } finally {
      setIsLoading(false);
    }
  }, [profileID, session]);
  
  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);
  
  const fetchUserCliques = useCallback(async () => {
    try {
      const userId = profileID ? Number(profileID) : session.userId;
      const result = await Routes.getCliques(session, { member: userId, format: 'profile' });
      
      if (result.ok) {
        const cliquesData = (result.data as Routes.CliqueProfile[]).map(clique => ({
          id: String(clique.id),
          title: clique.name,
          color: clusterColors.clusterYellow,
        }));
        
        setProfileSections(prev => prev.map(section => 
          section.id === 'joinedCliques' 
            ? { ...section, items: cliquesData.length > 0 ? cliquesData : section.items }
            : section
        ));
      }
    } catch (e) {
      console.error('Failed to fetch user cliques:', e);
    }
  }, [profileID, session]);
  
  const fetchFollowedClusters = useCallback(async () => {
    try {
      const userId = profileID ? Number(profileID) : session.userId;
      const result = await Routes.getClusters(session, { followed: userId, format: 'profile' });
      
      if (result.ok) {
        const clustersData = (result.data as Routes.ClusterProfile[]).map(cluster => ({
          id: String(cluster.id),
          title: cluster.title,
          color: clusterColors.clusterBlue,
        }));
        
        setProfileSections(prev => prev.map(section => 
          section.id === 'favoriteClusters' 
            ? { ...section, items: clustersData.length > 0 ? clustersData : section.items }
            : section
        ));
      }
    } catch (e) {
      console.error('Failed to fetch followed clusters:', e);
    }
  }, [profileID, session]);
  
  useEffect(() => {
    if (!isLoading && user.name) {
      fetchUserCliques();
      fetchFollowedClusters();
    }
  }, [isLoading, user.name, fetchUserCliques, fetchFollowedClusters]);

  // dynamic section data -- dont yell at me, i used ai for this, too much effort
  const [profileSections, setProfileSections] = useState<ProfileSection[]>([
    {
      id: 'likedSongs',
      title: 'Liked Songs',
      type: 'carousel',
      items: [
        { id: 's1', title: 'Midnight Dreams', color: clusterColors.clusterYellow },
        { id: 's2', title: 'Ocean Waves', color: '#086788' },
        { id: 's3', title: 'City Lights', color: '#FFE9C9' },
        { id: 's4', title: 'Sunrise', color: '#2a2a2a' },
      ],
      visibleItems: 3,
      visible: true,
      order: 0,
    },
    {
      id: 'joinedCliques',
      title: 'Joined Cliques',
      type: 'carousel',
      items: [
        { id: 'c1', title: 'Beat Makers', color: clusterColors.clusterYellow },
        { id: 'c2', title: 'Lo-Fi Lovers', color: '#086788' },
        { id: 'c3', title: 'Jazz Collective', color: '#FFE9C9' },
      ],
      visibleItems: 3,
      visible: true,
      order: 1,
    },
    {
      id: 'favoriteClusters',
      title: 'Favorite Clusters',
      type: 'carousel',
      items: [
        { id: 'cl1', title: 'Hip Hop', color: clusterColors.clusterYellow },
        { id: 'cl2', title: 'Electronic', color: '#086788' },
        { id: 'cl3', title: 'Jazz', color: '#FFE9C9' },
        { id: 'cl4', title: 'R&B', color: '#2a2a2a' },
      ],
      visibleItems: 3,
      visible: true,
      order: 2,
    },
    {
      id: 'achievements',
      title: 'Achievements',
      type: 'carousel',
      items: [
        { id: 'a1', title: 'First Beat', color: clusterColors.clusterYellow },
        { id: 'a2', title: 'Collaborator', color: '#FFE9C9' },
        { id: 'a3', title: 'Producer', color: '#086788' },
      ],
      visibleItems: 3,
      visible: true,
      order: 3,
    },
  ]);

  const [leaderboardSection, setLeaderboardSection] = useState({
    visible: true,
    order: 4,
  });

  const [leaderboardData, setLeaderboardData] = useState<LeaderboardEntry[]>([
    { id: 'l1', rank: 1, name: 'John Doe', location: 'Chapel Hill Rock', isUser: true },
    { id: 'l2', rank: 3, name: 'John Doe', location: 'Chapel Hill Rap', isUser: true },
    { id: 'l3', rank: 7, name: 'John Doe', location: 'Durham Electronic', isUser: true },
    { id: 'l4', rank: 2, name: 'John Doe', location: 'NC Hip Hop', isUser: true },
    { id: 'l5', rank: 15, name: 'John Doe', location: 'Global Lo-Fi', isUser: true },
  ]);

  const leaderboardItemWidth = SCREEN_WIDTH * 0.65;
  const carouselItemWidth = SCREEN_WIDTH * 0.2;
  const carouselItemMargin = SCREEN_WIDTH * 0.02;

  const [isFollowLoading, setIsFollowLoading] = useState(false);
  const [isJoinLoading, setIsJoinLoading] = useState(false);

  const Follow = async () => {
    if (!profileID || isFollowLoading) return;
    
    setIsFollowLoading(true);
    try {
      if (following) {
        const result = await Routes.unfollowUser(session, Number(profileID));
        if (result.ok) {
          setFollowing(false);
        } else {
          console.error('Failed to unfollow:', result.error);
        }
      } else {
        const result = await Routes.followUser(session, Number(profileID));
        if (result.ok) {
          setFollowing(true);
        } else {
          console.error('Failed to follow:', result.error);
        }
      }
    } catch (e) {
      console.error('Follow action failed:', e);
    } finally {
      setIsFollowLoading(false);
    }
  };

  // For clique profiles - join/leave functionality
  const Join = async () => {
    if (!profileID || isJoinLoading) return;
    
    setIsJoinLoading(true);
    try {
      if (joining === 'Member') {
        const result = await Routes.leaveClique(session, Number(profileID));
        if (result.ok) {
          setJoining('Join');
        } else {
          console.error('Failed to leave clique:', result.error);
        }
      } else if (joining === 'Join') {
        const result = await Routes.joinClique(session, Number(profileID));
        if (result.ok) {
          setJoining('Member');
        } else {
          console.error('Failed to join clique:', result.error);
        }
      }
    } catch (e) {
      console.error('Join action failed:', e);
    } finally {
      setIsJoinLoading(false);
    }
  };



  // Missing route - updateUserProfile(session, { biography })
  const changeBio = async () => {
    if(!isMe.current) return;
    
    setUser({...user, bio: editBio});
    
    // implement updateUserProfile(session, { biography })
  };

  // Missing route - updateProfileDisplaySettings(session, settings)
  const toggleSectionVisibility = (sectionId: string) => {
    if(!isMe.current || !isEditMode) return;

    setProfileSections(prevSections =>
      prevSections.map(section =>
        section.id === sectionId
          ? { ...section, visible: !section.visible }
          : section
      )
    );

    // implement updateProfileDisplaySettings(session, settings)
  };

  const toggleLeaderboardVisibility = () => {
    if(!isMe.current || !isEditMode) return;
    
    setLeaderboardSection(prev => ({
      ...prev,
      visible: !prev.visible
    }));
  };

  
  const loadMoreSectionData = (sectionId: string) => {
     // missing pagination support
  };

  const scrollLeaderboard = (direction: 'next' | 'prev') => {
    const newIndex = direction === 'next' 
      ? Math.min(currentLeaderboard + 1, leaderboardData.length - 1)
      : Math.max(currentLeaderboard - 1, 0);
    
    leaderboardScroll.current?.scrollToIndex({ 
      index: newIndex, 
      animated: true 
    });
    setCurrentLeaderboard(newIndex);
  };

  const scrollSection = (sectionId: string, direction: 'next' | 'prev') => {
    const section = profileSections.find(s => s.id === sectionId);
    if (!section || !sectionScrollRefs.current[sectionId]) return;

    const currentIndex = sectionScrollRefs.current[sectionId].currentIndex || 0;
    const newIndex = direction === 'next'
      ? Math.min(currentIndex + 1, section.items.length - section.visibleItems)
      : Math.max(currentIndex - 1, 0);

    sectionScrollRefs.current[sectionId].ref?.scrollToIndex({
      index: newIndex,
      animated: true
    });
    sectionScrollRefs.current[sectionId].currentIndex = newIndex;
  };


  
  const getProfileImageStyle = () => {
    switch (user.type.toLowerCase()) {
      case 'user':
        return styles.profileUser;
      case 'clique':
        return styles.profileClique;
      default:
        return styles.profileUser;
    }
  };

  const getImageStyle = () => {
    return user.type.toLowerCase() === 'cluster' ? styles.counterRotate : {};
  };

  const renderCarouselSection = (section: ProfileSection) => {
    return (
      <View key={section.id} style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>{section.title}</Text>
        <View style={styles.carouselWrapper}>
          <TouchableOpacity 
            style={styles.arrowButton}
            onPress={() => scrollSection(section.id, 'prev')}
          >
            <Text style={styles.arrowText}>‹</Text>
          </TouchableOpacity>

          <View style={styles.genericListContainer}>
            <FlatList
              ref={(ref) => {
                if (!sectionScrollRefs.current[section.id]) {
                  sectionScrollRefs.current[section.id] = { ref: null, currentIndex: 0 };
                }
                sectionScrollRefs.current[section.id].ref = ref;
              }}
              data={section.items}
              horizontal
              showsHorizontalScrollIndicator={false}
              snapToInterval={carouselItemWidth + carouselItemMargin}
              decelerationRate="fast"
              contentContainerStyle={styles.carouselContent}
              onEndReached={() => loadMoreSectionData(section.id)}
              onEndReachedThreshold={0.5}
              renderItem={({ item }) => (
                <View style={[styles.carouselItemWrapper, { width: carouselItemWidth }]}>
                  <View 
                    style={[
                      styles.carouselItem, 
                      { 
                        backgroundColor: item.color,
                        width: carouselItemWidth - 10,
                        height: carouselItemWidth - 10,
                        borderRadius: (carouselItemWidth - 10) / 2,
                      },
                    ]} 
                  />
                  <Text style={styles.carouselItemText} numberOfLines={2}>
                    {item.title}
                  </Text>
                </View>
              )}
              keyExtractor={(item) => item.id}
              getItemLayout={(data, index) => ({
                length: carouselItemWidth + carouselItemMargin,
                offset: (carouselItemWidth + carouselItemMargin) * index,
                index,
              })}
            />
          </View>

          <TouchableOpacity 
            style={styles.arrowButton}
            onPress={() => scrollSection(section.id, 'next')}
          >
            <Text style={styles.arrowText}>›</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  // edit stuff
  const renderSectionSettings = () => {
    if (!isMe.current || !isEditMode) return null;

    const allSections = [
      { id: 'leaderboard', title: 'Leaderboard', visible: leaderboardSection.visible, isLeaderboard: true },
      ...profileSections.map(s => ({ ...s, isLeaderboard: false }))
    ];

    return (
      <View style={styles.innerBox}>
        <Text style={styles.sectionTitle}>Section Visibility</Text>
        
        {allSections.map((section) => (
          <TouchableOpacity 
            key={section.id}
            style={[
              styles.settingItem,
              section.visible && styles.settingItemActive
            ]}
            onPress={() => section.isLeaderboard ? toggleLeaderboardVisibility() : toggleSectionVisibility(section.id)}
            activeOpacity={0.8}
          >
            <View style={containerStyles.genericRow}>
              <Text style={styles.checkIcon}>
                {section.visible ? '✓' : '×'}
              </Text>
              <Text style={objectStyles.subsectionTitle}>{section.title}</Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>
    );
  };

  if (isLoading) {
    return (
      <SafeAreaView style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size="large" color={clusterColors.clusterYellow} />
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={[styles.container, styles.centerContent]}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={fetchProfile}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style = {styles.row}>
        <TouchableOpacity onPress={()=> {router.replace("/settings")}}><Image
              source={require('@/assets/icons/settings.png')}
              style={styles.iconSettings}
            /></TouchableOpacity>
      </View>
      {/*
      <View style={styles.buttonRow}>
        <TouchableOpacity
          style={[
            styles.tabButton,
            selectedTab === 'profile' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
          ]}
          onPress={() => setSelectedTab('profile')}
        >
          <Text
            style={[
              objectStyles.tabText,
              selectedTab === 'profile' ? objectStyles.TextActive : objectStyles.TextInactive,
            ]}
          >
            My Profile
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.tabButton,
            selectedTab === 'achievements' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
          ]}
          onPress={() => setSelectedTab('achievements')}
        >
          <Text
            style={[
              objectStyles.tabText,
              selectedTab === 'achievements' ? objectStyles.TextActive : objectStyles.TextInactive,
            ]}
          >
            Achievements
          </Text>
        </TouchableOpacity>
      </View>
      */}
      <ScrollView 
        style={styles.innerBoxWrapper}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.innerBox}>
          <Text style={styles.title}>{user.name}</Text>
          
          <View style={[styles.profileContainer, getProfileImageStyle()]}>
            <Image
              source={{ uri: user.profileImage }}
              style={[styles.profileImageBase, getImageStyle()]}
            />
          </View>

          <Text style={styles.initStats}>
            {user.beats} Beats | {user.following} Following | {user.followers} Followers
          </Text>

          {!isMe.current && (
            <View style={styles.buttonContainer}>
              {(user.type.toLowerCase() === 'user' || user.type.toLowerCase() === 'clique') && (
                <TouchableOpacity
                  style={[
                    styles.followButton,
                    following ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
                    isFollowLoading && styles.buttonDisabled,
                  ]}
                  onPress={Follow}
                  disabled={isFollowLoading}
                >
                  {isFollowLoading ? (
                    <ActivityIndicator size="small" color={following ? clusterColors.fullWhite : clusterColors.clusterYellow} />
                  ) : (
                    <Text
                      style={[
                        objectStyles.tabText,
                        following ? objectStyles.TextActive : objectStyles.TextInactive,
                      ]}
                    >
                      {following ? 'Following' : 'Follow'}
                    </Text>
                  )}
                </TouchableOpacity>
              )}
              
              {user.type.toLowerCase() === 'clique' && (
                <TouchableOpacity
                  style={[
                    styles.followButton,
                    joining === 'Member' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
                    isJoinLoading && styles.buttonDisabled,
                  ]}
                  onPress={Join}
                  disabled={isJoinLoading}
                >
                  {isJoinLoading ? (
                    <ActivityIndicator size="small" color={joining === 'Member' ? clusterColors.fullWhite : clusterColors.clusterYellow} />
                  ) : (
                    <Text
                      style={[
                        objectStyles.tabText,
                        joining === 'Member' ? objectStyles.TextActive : objectStyles.TextInactive,
                      ]}
                    >
                      {joining === "Member" ? "Member" : joining === "Requested" ? "Requested": "Join"}
                    </Text>
                  )}
                </TouchableOpacity>
              )}
            </View>
          )}

          {isMe.current && (
            <TouchableOpacity
              style={[
                styles.editModeButton,
                isEditMode ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setIsEditMode(!isEditMode)}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  isEditMode ? objectStyles.TextActive: objectStyles.TextInactive, 
                ]}
              >
                {isEditMode ? 'Done Editing' : 'Edit Profile'}
              </Text>
            </TouchableOpacity>
          )}

          {isMe.current && isEditMode ? (
            <View style={styles.bioEditContainer}>
              <TextInput
                style={styles.bioInput}
                multiline
                value={editBio}
                onChangeText={setEditBio}
                placeholder="Enter your bio..."
                placeholderTextColor={clusterColors.fullWhite + '80'}
              />
              <TouchableOpacity 
                style={styles.bioSaveButton}
                onPress={changeBio}
              >
                <Text style={styles.bioSaveText}>Save Bio</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <TouchableOpacity onPress={() => setExpanded(!expanded)}>
              <Text style={styles.bio}>
                {expanded ? user.bio : user.bio.substring(0, 150) + '...'}
              </Text>
            </TouchableOpacity>
          )}
        </View>


        {renderSectionSettings()}

        {leaderboardSection.visible && (
          <View style={styles.innerBox}>
            <Text style={styles.sectionTitle}>Leaderboard</Text>
            <View style={styles.leaderboardWrapper}>
              <TouchableOpacity 
                style={styles.arrowButton}
                onPress={() => scrollLeaderboard('prev')}
              >
                <Text style={styles.arrowText}>‹</Text>
              </TouchableOpacity>

              <View style={styles.genericListContainer}>
                <FlatList
                  ref={leaderboardScroll}
                  data={leaderboardData}
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  snapToAlignment="start"
                  snapToInterval={leaderboardItemWidth}
                  decelerationRate="fast"
                  contentContainerStyle={styles.leaderboardContent}
                  onEndReached={() => loadMoreSectionData('leaderboard')}
                  onEndReachedThreshold={0.5}
                  renderItem={({ item }) => (
                    <View style={[styles.leaderboardItem, { width: leaderboardItemWidth }]}>
                      <View style={styles.leaderboardAvatar} />
                      <View style={styles.leaderboardInfo}>
                        <Text style={styles.leaderboardRank}>#{item.rank}</Text>
                        <Text style={styles.leaderboardLocation}>in {item.location}</Text>
                      </View>
                    </View>
                  )}
                  keyExtractor={(item) => item.id}
                  onMomentumScrollEnd={(e) => {
                    const index = Math.round(e.nativeEvent.contentOffset.x / leaderboardItemWidth);
                    setCurrentLeaderboard(index);
                  }}
                  getItemLayout={(data, index) => ({
                    length: leaderboardItemWidth,
                    offset: leaderboardItemWidth * index,
                    index,
                  })}
                />
              </View>

              <TouchableOpacity 
                style={styles.arrowButton}
                onPress={() => scrollLeaderboard('next')}
              >
                <Text style={styles.arrowText}>›</Text>
              </TouchableOpacity>
            </View>
            <TouchableOpacity style={styles.leaderboardButton} onPress={() => router.push('/leaderboard')}>
              <Text style={styles.leaderboardButtonText}>Leaderboard</Text>
            </TouchableOpacity>
          </View>
        )}

        {profileSections
          .filter(section => section.visible)
          .sort((a, b) => a.order - b.order)
          .map(section => (
            <View key={section.id} style={styles.innerBox}>
              {renderCarouselSection(section)}
            </View>
          ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    ...containerStyles.container,
    alignItems: 'center',
  },
  buttonRow: {
    ...containerStyles.buttonRow,
    marginTop: 20,
  },
  tabButton: {
    flex: 1,
    marginHorizontal: 5,
    borderRadius: 9,
    paddingVertical: 10,
    alignItems: 'center',
  },
  innerBoxWrapper: {
    flex: 1,
    paddingTop: 10,
    paddingBottom: 0,
    width: '90%',
    alignSelf: 'center',
  },  
  innerBox: {
    ...containerStyles.innerBox,
    flex: 0, 
    marginBottom: 15,
    overflow: 'hidden',
  },
  profileContainer: {
    width: 152,
    height: 152,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 10,
  },
  profileUser: {
    borderRadius: 76,
  },
  profileClique: {
    borderRadius: 57,
    transform: [{ rotate: '45deg' }],
  },
  profileImageBase: {
    width: 152,
    height: 152,
  },
  counterRotate: {
    transform: [{ rotate: '-45deg' }],
  },
  title: {
    ...objectStyles.title,
    margin: 0
  },
  initStats: {
    padding: 5,
    color: clusterColors.fullWhite,
    fontFamily: "FuturaPT-Med",
    textAlign: 'center',
    fontSize: 13,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 15,
    justifyContent: 'center',
  },
  followButton: {
    borderRadius: 9,
    paddingVertical: 10,
    paddingHorizontal: 30,
    alignItems: 'center',
    minWidth: 120,
  },
  editModeButton: {
    marginTop: 15,
    borderRadius: 9,
    paddingVertical: 10,
    paddingHorizontal: 30,
    alignItems: 'center',
    minWidth: 120,
  },

  bio: {
    paddingTop: 5,
    textAlign: 'center',
    color: clusterColors.fullWhite,
    fontSize: 12,
    fontFamily: "FuturaPT-Light",
    lineHeight: 15,
  },
  bioEditContainer: {
    width: '100%',
    marginTop: 10,
  },
  bioInput: {
    color: clusterColors.fullWhite,
    fontSize: 12,
    fontFamily: "FuturaPT-Light",
    lineHeight: 15,
    textAlign: 'center',
    minHeight: 60,
    borderWidth: 1,
    borderColor: clusterColors.clusterYellow,
    borderRadius: 9,
    padding: 10,
    marginBottom: 10,
  },
  bioSaveButton: {
    backgroundColor: clusterColors.clusterYellow,
    borderRadius: 9,
    paddingVertical: 8,
    paddingHorizontal: 20,
    alignSelf: 'center',
  },
  bioSaveText: {
    color: clusterColors.fullWhite,
    fontSize: 12,
    fontFamily: "FuturaPT-Bold",
  },
  settingItem: {
    ...containerStyles.genericRow,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#3a3a3a',
  },
  settingItemActive: {
    flex: 0,
    backgroundColor: '#333',
  },
  checkIcon: {
    width: 35,
    color: clusterColors.clusterYellow,
    fontSize: 18,
    fontFamily: "FuturaPT-Bold",
    marginRight: 10,
    textAlign: 'center',
  },
  sectionContainer: {
    width: '100%',
  },
  sectionTitle: {
    ...objectStyles.sectionTitle, 
    padding: 0,
    fontSize: 18,
    marginBottom: 15,
  },
  carouselWrapper: {
    ...containerStyles.genericRow,
    justifyContent: 'center',
    width: '100%',
  },
  leaderboardWrapper: {
    ...containerStyles.genericRow,
    justifyContent: 'flex-start',
    width: '100%',
  },
  genericListContainer: {
    flex: 1,
    overflow: 'hidden',
  },
  arrowButton: {
    padding: 10,
    justifyContent: 'center',
    alignItems: 'center',
    width: 40,
  },
  arrowText: {
    color: clusterColors.clusterYellow,
    fontSize: 32,
    fontWeight: 'bold',
  },
  carouselContent: {
    alignItems: 'center',
    paddingHorizontal: 5,
  },
  carouselItemWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  carouselItem: {
    marginBottom: 5,
  },
  carouselItemText: {
    color: '#FFF',
    fontSize: 10,
    textAlign: 'center',
    marginTop: 5,
    fontFamily: "FuturaPT-Book",
  },
  leaderboardContent: {
    alignItems: 'flex-start',
  },
  leaderboardItem: {
    ...containerStyles.genericRow,
    flex: 0,
    justifyContent: 'flex-start',
    paddingVertical: 20,
    paddingHorizontal: 10,
  },
  leaderboardAvatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: clusterColors.clusterYellow,
    marginRight: 15,
  },
  leaderboardInfo: {
    alignItems: 'flex-start',
  },
  leaderboardRank: {
    color: clusterColors.fullWhite,
    fontSize: 32,
    fontWeight: 'bold',
  },
  leaderboardLocation: {
    color: clusterColors.fullWhite,
    fontSize: 12,
    opacity: 0.7,
  },
  leaderboardButton: {
    backgroundColor: clusterColors.clusterYellow,
    borderRadius: 9,
    paddingVertical: 8,
    paddingHorizontal: 20,
    marginTop: 10,
    alignSelf: 'flex-end',
  },
  leaderboardButtonText: {
    color: clusterColors.fullWhite,
    fontSize: 10,
    fontFamily: "FuturaPT-Bold",
  },
  iconSettings: {
    ...objectStyles.genericIcon,
    marginRight: 20,
    //marginBottom: -15,
    padding:10,
    alignSelf: 'flex-end',
    width: 40,
    height:40,
  },
  row: {
    flex: 0,
    flexDirection: 'row',
    justifyContent: 'flex-end',  
    width: '100%',                
    paddingHorizontal: 20,        
  },
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
  buttonDisabled: {
    opacity: 0.6,
  },
});
