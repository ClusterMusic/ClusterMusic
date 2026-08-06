import { clusterColors, containerStyles, objectStyles } from '@/constants/style';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from "react";
import {
  FlatList,
  Image,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

interface Comment {
  comment_id: string;
  parent_id: string;
  comment_poster: string;
  text: string;
  replies: string[];
  profileImage: string | null;
  date: string;
}

// Flat storage of ALL comments - lookup by comment_id. 
const ALL_COMMENTS: { [key: string]: Comment } = {
  'comment_root': {
    comment_id: 'comment_root',
    parent_id: 'cluster',
    comment_poster: 'Comment Poster',
    text: 'First Comment goes here. This is the root comment shown on the cluster.',
    replies: ['comment_abc123', 'comment_xyz789', 'comment_def456'],
    profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA",
    date: "2024-01-15"
  },
  'comment_abc123': {
    comment_id: 'comment_abc123',
    parent_id: 'comment_root',
    comment_poster: 'User123',
    text: 'This is a great point! I totally agree with what you said.',
    replies: ['comment_reply1', 'comment_reply2'],
    profileImage: null,
    date: "2024-01-16"
  },
  'comment_xyz789': {
    comment_id: 'comment_xyz789',
    parent_id: 'comment_root',
    comment_poster: 'MusicLover',
    text: 'Interesting perspective on this track.',
    replies: ['comment_music1'],
    profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA",
    date: "2024-01-17"
  },
  'comment_def456': {
    comment_id: 'comment_def456',
    parent_id: 'comment_root',
    comment_poster: 'JazzFan',
    text: 'The production quality on this album is incredible.',
    replies: ['comment_producer1'],
    profileImage: null,
    date: "2024-01-18"
  },
  'comment_reply1': {
    comment_id: 'comment_reply1',
    parent_id: 'comment_abc123',
    comment_poster: 'Another User',
    text: 'Thanks for sharing your thoughts!',
    replies: ['comment_deep1'],
    profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA",
    date: "2024-01-19"
  },
  'comment_reply2': {
    comment_id: 'comment_reply2',
    parent_id: 'comment_abc123',
    comment_poster: 'Someone Else',
    text: 'I have a different take on this...',
    replies: [],
    profileImage: null,
    date: "2024-01-20"
  },
  'comment_music1': {
    comment_id: 'comment_music1',
    parent_id: 'comment_xyz789',
    comment_poster: 'TrackEnthusiast',
    text: 'Completely agree! The melodies are so catchy.',
    replies: [],
    profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA",
    date: "2024-01-21"
  },
  'comment_producer1': {
    comment_id: 'comment_producer1',
    parent_id: 'comment_def456',
    comment_poster: 'Producer',
    text: 'Thank you! We worked really hard on the mix.',
    replies: [],
    profileImage: null,
    date: "2024-01-22"
  },
  'comment_deep1': {
    comment_id: 'comment_deep1',
    parent_id: 'comment_reply1',
    comment_poster: 'Deep Reply User',
    text: 'This thread is getting interesting!',
    replies: [],
    profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA",
    date: "2024-01-23"
  }
};

const renderReply = ({ item }: { item: Comment }) => {
  const router = useRouter();
  return (
    <TouchableOpacity 
      onPress={() => router.replace(`/comments/${item.comment_id}`)}
      activeOpacity={0.7}
    >
      <View style={styles.replyRow}>
        <Image 
          source={item.profileImage ? { uri: item.profileImage } : require('@/assets/icons/profile.png')}
          style={styles.profileIcon} 
        />
        <View style={styles.replyBubble}>
          <View style={styles.replyHeader}>
            <Text style={styles.replyAuthor}>{item.comment_poster}</Text>
            <Text style={styles.replyDate}>{item.date}</Text>
          </View>
          <Text style={styles.replyText}>{item.text}</Text>
          {item.replies.length > 0 && (
            <Text style={styles.replyCount}>
              {`${item.replies.length} ${item.replies.length === 1 ? 'reply' : 'replies'}`}
            </Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
};

export default function CommentsPage() {
  const { commentId } = useLocalSearchParams();
  const current_comment_id = (commentId as string) || 'comment_root';
  
  const currentComment = ALL_COMMENTS[current_comment_id];
  const [newReply, setNewReply] = useState("");
  const router = useRouter();

  // Get the actual comment objects for all replies
  const replyComments = currentComment.replies.map(reply_id => ALL_COMMENTS[reply_id]).filter(Boolean);

  const handleSend = () => {
    console.log("Sending reply: ", newReply);
    setNewReply("");
  };

  const handleBackPress = () => {
    if (currentComment.parent_id === 'cluster') {
      router.replace('/');
    } else {
      router.replace(`/comments/${currentComment.parent_id}`);
    }
  };

  return (
    <SafeAreaView style={containerStyles.container}>
      <View style={styles.upperBox}>
        <TouchableOpacity onPress={handleBackPress} style={styles.backButton}>
          <Image
            source={require('@/assets/icons/back.png')}
            style={styles.backIcon}
          />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>
          {current_comment_id === 'comment_root' ? 'Comments' : 'Replies'}
        </Text>
      </View>

      <View style={styles.parentCommentContainer}>
        <View style={styles.parentCommentRow}>
          <Image 
            source={currentComment.profileImage ? { uri: currentComment.profileImage } : require('@/assets/icons/profile.png')}
            style={styles.profileIcon} 
          />
          <View style={styles.parentCommentBubble}>
            <View style={styles.parentCommentHeader}>
              <Text style={styles.parentCommentAuthor}>{currentComment.comment_poster}</Text>
              <Text style={styles.parentCommentDate}>{currentComment.date}</Text>
            </View>
            <Text style={styles.parentCommentText}>{currentComment.text}</Text>
          </View>
        </View>
      </View>

      <FlatList
        data={replyComments}
        renderItem={renderReply}
        keyExtractor={(item) => item.comment_id}
        contentContainerStyle={{ padding: 10 }}
        ListEmptyComponent={
          <Text style={styles.emptyText}>No replies yet. Be the first to reply!</Text>
        }
      />

      <View style={styles.inputBox}>
        <TextInput
          style={styles.textBox}
          placeholder="Write a reply..."
          placeholderTextColor="#FFE9C9"
          value={newReply}
          onChangeText={setNewReply}
        />
        
        <TouchableOpacity onPress={handleSend}>
          <Text style={styles.sendButton}>Send</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  upperBox: {
    ...containerStyles.genericRow, 
    marginHorizontal: 15,
    marginTop: 15,
    marginBottom: 5,
    paddingBottom: 10,
    borderBottomWidth: 2,
    borderBottomColor: clusterColors.clusterYellow,
  },

  backButton: {
    marginRight: 10,
    padding: 5,
  },

  backIcon: {
    ...objectStyles.genericIcon, 
    width: 20,
    height: 20,
    resizeMode: 'contain',
  },

  headerTitle: {
    color: '#FFF',
    fontFamily: 'FuturaPT-Bold',
    fontSize: 16,
    padding: 5,
  },

  parentCommentContainer: {
    backgroundColor: clusterColors.clusterYellow,
    paddingVertical: 15,
    paddingHorizontal: 10,
    borderBottomWidth: 2,
  },

  parentCommentRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },

  profileIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 10,
  },

  parentCommentBubble: {
    flex: 1,
  },

  parentCommentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 5,
  },

  parentCommentAuthor: {
    ...objectStyles.TextActive,
    fontFamily: 'FuturaPT-Bold',
  },

  parentCommentDate: {
    ...objectStyles.TextActive,
    fontFamily: 'FuturaPT-Book',
    fontSize: 10,
    opacity: 0.7,
  },

  parentCommentText: {
    ...objectStyles.TextActive,
    fontFamily: 'FuturaPT-Book',
    lineHeight: 18,
  },

  replyRow: {
    flexDirection: 'row',
    marginVertical: 8,
    alignItems: 'flex-start',
  },

  replyBubble: {
    flex: 1,
    backgroundColor: clusterColors.clusterTint,
    padding: 12,
    borderRadius: 9,
  },

  replyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },

  replyAuthor: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Bold',

  },

  replyDate: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Book',
    fontSize: 9,
    opacity: 0.6,
  },

  replyText: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Book',
    lineHeight: 16,
  },

  replyCount: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Med',
    fontSize: 10,
    marginTop: 6,
    opacity: 0.7,
  },

  emptyText: {
    ...objectStyles.smallText,
    color: clusterColors.fullWhite,
    textAlign: 'center',
    marginTop: 30,
    opacity: 0.6,
  },

  inputBox: {
    ...containerStyles.genericRow,
    padding: 10,
    gap: 10
  },

  textBox: {
    ...objectStyles.textBox,
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: clusterColors.clusterTint,
    paddingVertical: 15,
    paddingHorizontal: 15,
    fontSize: 16,
  },

  sendButton: {
    color: clusterColors.clusterYellow,
    fontWeight: 'bold',
    fontSize: 16,
    paddingHorizontal: 8,
  }
});
