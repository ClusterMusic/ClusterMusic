import { useAuth } from '@/components/auth';
import ClusterComponent from "@/components/cluster-box";
import * as Routes from '@/components/routes';
import { containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Dimensions, FlatList, Image, RefreshControl, StyleSheet, Text, TouchableOpacity, useWindowDimensions, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

const { width, height } = Dimensions.get("window")

export default function App() {
const { height } = useWindowDimensions();
const [headerHeight, setHeaderHeight] = useState(0);
const itemHeight = height - headerHeight;
 const { session } = useAuth();
 const [posts, setPosts] = useState([]); // Posts - Display posts
 const [isLoading, setIsLoading] = useState(true); // Loading - Display loading indicator
 const [isRefreshing, setIsRefreshing] = useState(false); // Refreshing - Display refresh indicator
 const [error, setError] = useState(null); // Error - Display error message
 const [currentIndex, setCurrentIndex] = useState(0); 

 const fetchPosts = useCallback(async () => { // Fetch posts - Get posts from the database
  try {
   setError(null);
   const result = await Routes.getForYouRadio(session, 20);
   
   if (!result.ok) {
    setError(result.error);
    return;
   }
   
   setPosts(result.data.map((post) => ({
    id: String(post.id),
    title: post.song.title,
    album: post.song.album,
    author: post.song.artist,
    bio: post.caption,
    tag: `${post.poster.username} | ${post.clique.name} | ${post.cluster.title}`,
    image: post.song.image,
    postId: post.id,
    songId: post.song.id,
   })));
  } catch (e) {
   setError(e.message || 'Failed to load feed');
  } finally {
   setIsLoading(false);
   setIsRefreshing(false);
  }
 }, [session]);

 useEffect(() => {
  fetchPosts();
 }, [fetchPosts]);

 const onRefresh = useCallback(() => {
  setIsRefreshing(true);
  fetchPosts();
 }, [fetchPosts]);

 // Track post views when scrolling
 const onViewableItemsChanged = useCallback(({ viewableItems }) => {
  if (viewableItems.length > 0) {
   const item = viewableItems[0].item;
   setCurrentIndex(viewableItems[0].index);
   // Record the view for this post
   Routes.viewPost(session, item.postId).catch(console.error);
  }
 }, [session]);

 if (isLoading) {
  return (
   <SafeAreaView style={[containerStyles.container, styles.centerContent]}>
    <ActivityIndicator size="large" color="#EFA00B" />
   </SafeAreaView>
  );
 }

 if (error) {
  return (
   <SafeAreaView style={[containerStyles.container, styles.centerContent]}>
    <Text style={styles.errorText}>{error}</Text>
    <TouchableOpacity style={styles.retryButton} onPress={fetchPosts}>
     <Text style={styles.retryText}>Retry</Text>
    </TouchableOpacity>
   </SafeAreaView>
  );
 }

 return (
  <SafeAreaView style={containerStyles.container}>
   <View onLayout={(e) => setHeaderHeight(e.nativeEvent.layout.height)}>
      <TouchableOpacity style={styles.searchButton} onPress={()=>{router.replace('/search')}}>
        <Image
          source={require('@/assets/icons/search.png')}
          style={styles.searchIcon}
        />
      </TouchableOpacity>
    </View>

   <FlatList
  data={posts}
  keyExtractor={(item) => item.id}
  pagingEnabled
  decelerationRate="fast"
  snapToInterval={itemHeight}
  snapToAlignment="start"
  showsVerticalScrollIndicator={false}
  getItemLayout={(_, index) => ({
    length: itemHeight,
    offset: itemHeight * index,
    index,
  })}
  renderItem={({ item }) => (
    <View style={[styles.postContainer, { height: itemHeight }]}>
      <ClusterComponent
       id={item.id}
       title={item.title}
       album={item.album}
       author={item.author}
       bio={item.bio}
       tag={item.tag}
       image={item.image}
      />
     </View>
    )}
    horizontal
    showsHorizontalScrollIndicator={false}



    onViewableItemsChanged={onViewableItemsChanged}
    viewabilityConfig={{ itemVisiblePercentThreshold: 50 }}
    refreshControl={
     <RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} tintColor="#EFA00B" />
    }
    ListEmptyComponent={
     <View style={[styles.centerContent, { width, height: height * 0.6 }]}>
      <Text style={styles.emptyText}>No posts in your feed</Text>
     </View>
    }
   />
  </SafeAreaView>
 )
}

const styles = StyleSheet.create({
 postContainer: {
  flex: 1,
  width: '100%',
 },
 searchIcon: {
  ...objectStyles.genericIcon, 
  marginRight: 10, 
  alignSelf: "flex-end",
 },
 centerContent: {
  flex: 1,
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
  backgroundColor: '#EFA00B',
  paddingHorizontal: 24,
  paddingVertical: 12,
  borderRadius: 9,
 },
 retryText: {
  color: '#FFF',
  fontFamily: 'FuturaPT-Bold',
  fontSize: 14,
 },
 emptyText: {
  color: '#CCC',
  fontSize: 16,
  fontFamily: 'FuturaPT-Book',
 },
})
