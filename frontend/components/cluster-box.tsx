import { clusterColors, containerStyles, objectStyles } from '@/constants/style'
import { router } from 'expo-router'
import { useRef, useState } from 'react'
import { Image, NativeScrollEvent, NativeSyntheticEvent, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'

interface ClusterProps {
 title: string
 album: string
 author: string
 bio: string
 tag: string
 comment_id: string
}

export default function ClusterComponent({ title, album, author, bio, tag, comment_id }: ClusterProps) {
  const insets = useSafeAreaInsets()
  const bottomPadding = 150 + insets.bottom
  const bioScrollRef = useRef<ScrollView>(null)
  const [isAtBottom, setIsAtBottom] = useState(false)
  const [currentScrollY, setCurrentScrollY] = useState(0)
  
  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent
    const atBottom = contentOffset.y + layoutMeasurement.height >= contentSize.height - 5
    setIsAtBottom(atBottom)
    setCurrentScrollY(contentOffset.y)
  }
  
  const handleTap = () => {
    if (isAtBottom) {
      bioScrollRef.current?.scrollTo({
        y: 0,
        animated: true,
      })
      setIsAtBottom(false)
    } else {
      bioScrollRef.current?.scrollTo({
        y: currentScrollY + 80,
        animated: true,
      })
    }
  }

  const onPlay = () => {

  }
  
  return (
    <View style={containerStyles.container}>
      <View style={[containerStyles.innerBoxWrapper, { paddingBottom: bottomPadding }]}>
        <View style={styles.innerBox }>
          
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={styles.profileImage} />
            <Text style={styles.tagText}>{tag}</Text>
          </View>

          
          <View style={{ alignItems: 'center', margin: 5 }}>
            <TouchableOpacity>
              <View style={styles.vinylPlaceholder}>
                <View style={styles.innerShadow} />
                <Image
                  source={require('@/assets/icons/circle_play.png')}
                              />
              </View> 
              </TouchableOpacity>
            
          </View>

          {/* Info */}
          <Text style={objectStyles.title}>{title}</Text>
          <Text style={styles.album}>{album}</Text>
          <Text style={styles.author}>{author}</Text>

          <View style={styles.flexFill}>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={handleTap}
            >
              <ScrollView
                ref={bioScrollRef}
                scrollEnabled={true}
                showsVerticalScrollIndicator={false}
                contentContainerStyle={styles.bioScrollContent}
                onScroll={handleScroll}
                scrollEventThrottle={16}
              >
                <Text style={styles.bio}>{bio}</Text>
              </ScrollView>
            </TouchableOpacity>
          </View>

          <TouchableOpacity onPress = {()=> {router.replace('/comments/')}}>
            <View style={styles.commentBox}>
              <Text style={styles.commentAuthor}>Comment Poster</Text>
              <Text style={styles.commentText}>First Comment goes here</Text>
            </View>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({

  innerBox: {
    ...containerStyles.innerBox,
    alignItems: 'stretch', 
  },

 vinylPlaceholder: {
  width: 237,
  height: 237,
  borderRadius: 118.5,
  backgroundColor: clusterColors.clusterYellow,
  justifyContent: "center",
  alignItems: "center",
  overflow: "hidden", // REQUIRED
},

innerShadow: {
  position: "absolute",
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  borderRadius: 118.5,
  borderWidth: 10,
  borderColor: "rgba(0,0,0,0.25)",
},
 profileImage: {
  backgroundColor: clusterColors.fullWhite,
  width: 45,
  height: 45,
  borderRadius: 22.5,
  flexShrink: 0,
 },

 album: {
  ...objectStyles.subsectionTitle,
  padding: 5,
  textAlign: 'center',
 },

 author: {
  ...objectStyles.TextActive,
  padding:5,
  textAlign: 'center',
  fontSize: 16,
 },

 tagText: {
  padding: 10,
  color: clusterColors.fullWhite,
  fontSize: 14,
  fontFamily: 'FuturaPT-Med',
 },
 flexFill: {
  flex: 1,
  justifyContent: 'flex-start',
 },
 bioScrollContent: {
  flexGrow: 1,
  paddingBottom: 10,
 },
 bio: {
  color: clusterColors.fullWhite,
  fontSize: 10,
  fontWeight: '100',
  fontFamily: 'FuturaPT-Book',
  lineHeight: 15,
 },
 commentBox: {
  backgroundColor: clusterColors.clusterTint,
  borderRadius: 5,
  padding: 10,
  paddingBottom: 25,
  marginTop: 10,
 },
 commentAuthor: {
  color: clusterColors.fullBlack,
  fontSize: 12,
  fontFamily: 'FuturaPT-Bold',
  lineHeight: 14,
 },
 commentText: {
  color: clusterColors.fullBlack,
  fontSize: 12,
  fontFamily: 'FuturaPT-Book',
  lineHeight: 14,
 },
})
