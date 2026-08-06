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

// Implenment chat functionality

interface Message {
  id: string;
  author: string;
  message: string;
  isUser: boolean;
  profileImage?: string | null;
}

interface ChatInfo {
  users: string[];
  messages: Message[];
  cliqueName: string;
}

const mockChatInfo: ChatInfo = {
  users: ['You', 'Alice'],
  cliqueName: 'Weekend Vibes',
  messages: [
    {
      id: '1',
      author: 'Alice',
      message: 'Hey, what is up? I just finished a long day of work and finally got some free time to relax. How about you?',
      isUser: false,
      profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA"
    },
    {
      id: '2',
      author: 'You',
      message: 'Not much, just catching up on some reading and planning to watch a movie later. Did you see the latest episode of that show?',
      isUser: true,
      profileImage: null
    },
    {
      id: '3',
      author: 'Alice',
      message: 'Yes! It was amazing. I loved how the plot twisted and kept me on the edge of my seat. Cannot wait for the next one.',
      isUser: false,
      profileImage: null
    },
    {
      id: '4',
      author: 'You',
      message: 'Not much, just catching up on some reading and planning to watch a movie later. Did you see the latest episode of that show?',
      isUser: true,
      profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA"
    },
    {
      id: '5',
      author: 'You',
      message: 'Not much, just catching up on some reading and planning to watch a movie later. Did you see the latest episode of that show?',
      isUser: true,
      profileImage: null
    },
    {
      id: '6',
      author: 'Alice',
      message: 'Yes! It was amazing. I loved how the plot twisted and kept me on the edge of my seat. Cannot wait for the next one.',
      isUser: false,
      profileImage: "https://fastly.picsum.photos/id/1051/200/200.jpg?hmac=s6d4ypEjpec8nvA2zqhWzx_6ogXYM2fJ_YJwaOM1CUA"
    },
    {
      id: '7',
      author: 'Alice',
      message: 'Yes! It was amazing. I loved how the plot twisted and kept me on the edge of my seat. Cannot wait for the next one.',
      isUser: false,
      profileImage: null
    }
  ]
};

const renderItem = ({ item }: { item: Message }) => {
  return (
    <View style={[styles.messageRow, item.isUser ? styles.userRow : styles.otherRow]}>
      {!item.isUser && (
        <Image 
          source={item.profileImage ? { uri: item.profileImage } : require('@/assets/icons/profile.png')} 
          style={styles.profileIcon} 
        />
      )}
      <View style={[styles.messageBubble, item.isUser ? styles.userBubble : styles.otherBubble]}>
        <Text style={[styles.messageAuthor, item.isUser ? styles.userAuthorText : styles.otherAuthorText]}>
          {item.author}
        </Text>
        <Text style={styles.messageText}>{item.message}</Text>
      </View>
    </View>
  );
};

export default function Chat() {
  const { chatID } = useLocalSearchParams(); 
  const [chatInfo] = useState<ChatInfo>(mockChatInfo);
  const [newMessage, setNewMessage] = useState("");
  const router = useRouter();
  
  const handleSend = () => {
    console.log("Sending message: ", newMessage);
    setNewMessage("");
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.upperBox}>
        <TouchableOpacity onPress={() => router.replace("/clique")} style={styles.backButton}>
          <Image
            source={require('@/assets/icons/back.png')}
            style={styles.backIcon}
          />
        </TouchableOpacity>
        <Text style={styles.cliqueName}>{chatInfo.cliqueName}</Text>
      </View>

      <FlatList
        data={chatInfo.messages}
        renderItem={renderItem}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 10 }}
        inverted
      />

      <View style={styles.inputBox}>
        <TouchableOpacity style={styles.uploadButton}>
          <Text style={styles.uploadText}>+</Text>
        </TouchableOpacity>

        <TextInput
          style={styles.textBox}
          placeholder="Type your message..."
          placeholderTextColor="#FFE9C9"
          value={newMessage}
          onChangeText={setNewMessage}
        />
        
        <TouchableOpacity onPress={handleSend}>
          <Text style={styles.sendButton}>Send</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { 
    flex: 1 
  },

  upperBox: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 15,
    marginTop: 15,
    marginBottom: 5,
    paddingBottom: 10,
    borderBottomWidth: 2,
    borderBottomColor: '#EFA00B',
  },

  backButton: {
    marginRight: 10,
    padding: 5,
  },

  backIcon: {
    width: 20,
    height: 20,
    resizeMode: 'contain',
    tintColor: '#FFE9C9' 
  },

  cliqueName: {
    color: '#FFF',
    fontFamily: 'FuturaPT-Bold',
    fontSize: 16,
    padding: 5,
  },

  messageRow: {
    flexDirection: 'row',
    marginVertical: 5,
    alignItems: 'flex-end'
  },
  
  userRow: { 
    justifyContent: 'flex-end' 
  },
  
  otherRow: { 
    justifyContent: 'flex-start' 
  },

  profileIcon: {
    width: 35,
    height: 35,
    borderRadius: 17.5,
    marginRight: 5
  },

  messageBubble: {
    maxWidth: '75%',
    padding: 10,
    borderRadius: 9
  },
  
  userBubble: { 
    backgroundColor: '#EFA00B' 
  },
  
  otherBubble: { 
    backgroundColor: '#FFE9C9' 
  },

  messageAuthor: {
    marginBottom: 2,
    fontWeight: 'bold'
  },
  
  userAuthorText: {
    fontFamily: 'FuturaPT-Bold',
    color: '#FFF'
  },
  
  otherAuthorText: {
    color: '#000',
    fontFamily: 'FuturaPT-Bold',
  },
  
  messageText: {
    color: '#000',
    fontFamily: 'FuturaPT-Book',
  },

  inputBox: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10,
    gap: 10
  },

  textBox: {
    flex: 1,
    borderRadius: 9,
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: '#FFE9C9',
    fontFamily: 'FuturaPT-Book',
    color: '#fff',
    paddingVertical: 15,
    paddingHorizontal: 15,
    fontSize: 16,
  },

  uploadButton: {
    padding: 10
  },

  uploadText: {
    fontSize: 24,
    color: '#FFE9C9'
  },

  sendButton: {
    color: '#EFA00B',
    fontWeight: 'bold',
    fontSize: 16,
    paddingHorizontal: 8
  }
});
