import { containerStyles, objectStyles } from '@/constants/style';
import { router } from 'expo-router';
import { useState } from 'react';
import { FlatList, Image, StyleSheet, TouchableOpacity, useWindowDimensions, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import ClusterComponent from "../../components/cluster-box";
const mockData = [
 { id: "1", title: "Runaway", album: "My Beautiful Dark Twisted Fantasy", author: "Kanye West", tag: "Post Author | Cluster | Promoted Clique", 
  bio: "Finding the red rose in the mailbox was a pleasant surprise for Sarah. She didn't have a boyfriend or know of anyone who was interested in her as anything more than a friend. There wasn't even a note attached to it. Although it was a complete mystery, it still made her heart jump and race a little more than usual. She wished that she could simply accept the gesture and be content knowing someone had given it to her, but that wasn't the way Sarah did things. Now it was time to do a little detective work and try to figure who had actually left the red rose. know of anyone who was interested in her as anything more than a friend. There wasn't even a note attached to it. Although it was a complete mystery, it still made her heart jump and race a little more than usual. She wished that she could simply accept the gesture and be content knowing someone had given it to her, but that wasn't the way Sarah did things. Now it was time to do a little detective work and try to figure who had actually left the red rose. know of anyone who was interested in her as anything more than a friend. There wasn't even a note attached to it. Although it was a complete mystery, it still made her heart jump and race a little more than usual. She wished that she could simply accept the gesture and be content knowing someone had given it to her, but that wasn't the way Sarah did things. Now it was time to do a little detective work and try to figure who had actually left the red rose." },
 { id: "2", title: "Nights", album: "Blonde", author: "Frank Ocean", tag: "Post Author | Cluster | Promoted Clique", bio: "The sky dimmed and the neon lights of the city started to pulse through the mist..." },
 { id: "3", title: "Pink + White", album: "Blonde", author: "Frank Ocean", tag: "Post Author | Cluster | Promoted Clique", bio: "Waves crash, the sand sticks to her skin, and all that’s left is memory..." },
]

export default function FollowingPage() {
const { height } = useWindowDimensions();
const [headerHeight, setHeaderHeight] = useState(0);
const itemHeight = height - headerHeight;
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
  data={mockData}
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
      />
    </View>
  )}
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
})
