import { clusterColors, containerStyles, objectStyles } from "@/constants/style";
import { useState } from "react";
import { FlatList, Image, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

// Define types
interface LeaderboardEntry {
  rank: number;
  name: string;
  beats: number;
}

type TabName = 'Global' | 'Users' | 'Cliques';

// Mock data
const mockData: Record<TabName, LeaderboardEntry[]> = {
  Global: [
    { rank: 1, name: "CLIQUE 1", beats: 5933 },
    { rank: 2, name: "CLIQUE 2", beats: 5821 },
    { rank: 3, name: "CLIQUE 3", beats: 5654 },
    { rank: 4, name: "CLIQUE 4", beats: 5432 },
    { rank: 5, name: "CLIQUE 5", beats: 5201 },
    { rank: 6, name: "CLIQUE 6", beats: 4987 },
    { rank: 7, name: "CLIQUE 7", beats: 4756 },
    { rank: 8, name: "CLIQUE 8", beats: 4623 },
    { rank: 9, name: "CLIQUE 9", beats: 4501 },
    { rank: 10, name: "CLIQUE 10", beats: 4389 },
    { rank: 11, name: "CLIQUE 11", beats: 4276 },
    { rank: 12, name: "CLIQUE 12", beats: 4143 },
    { rank: 13, name: "CLIQUE 13", beats: 4012 },
    { rank: 14, name: "CLIQUE 14", beats: 3901 },
    { rank: 15, name: "CLIQUE 15", beats: 3789 },
    { rank: 16, name: "CLIQUE 16", beats: 3654 },
    { rank: 17, name: "CLIQUE 17", beats: 3521 },
    { rank: 18, name: "CLIQUE 18", beats: 3412 },
    { rank: 19, name: "CLIQUE 19", beats: 3301 },
    { rank: 20, name: "CLIQUE 20", beats: 3198 },
  ],
  Users: [
    { rank: 1, name: "DJ_MASTER", beats: 2933 },
    { rank: 2, name: "BEAT_KING", beats: 2821 },
    { rank: 3, name: "RHYTHM_ACE", beats: 2654 },
    { rank: 4, name: "AUDIO_NINJA", beats: 2432 },
    { rank: 5, name: "SOUND_WAVE", beats: 2201 },
    { rank: 6, name: "MIX_MASTER", beats: 1987 },
    { rank: 7, name: "BASS_DROP", beats: 1756 },
    { rank: 8, name: "TRAP_LORD", beats: 1623 },
    { rank: 9, name: "SYNTH_GOD", beats: 1501 },
    { rank: 10, name: "LOOP_KING", beats: 1389 },
    { rank: 11, name: "BEAT_SMITH", beats: 1276 },
    { rank: 12, name: "SAMPLE_ACE", beats: 1143 },
    { rank: 13, name: "DROP_MASTER", beats: 1012 },
    { rank: 14, name: "WAVE_RIDER", beats: 901 },
    { rank: 15, name: "FREQ_BOSS", beats: 789 },
    { rank: 16, name: "ECHO_LEGEND", beats: 654 },
    { rank: 17, name: "VIBE_CHIEF", beats: 521 },
    { rank: 18, name: "TEMPO_KING", beats: 412 },
    { rank: 19, name: "GROOVE_LORD", beats: 301 },
    { rank: 20, name: "PITCH_PERFECT", beats: 198 },
  ],
  Cliques: [
    { rank: 1, name: "BEAT SQUAD", beats: 8933 },
    { rank: 2, name: "RHYTHM CREW", beats: 8421 },
    { rank: 3, name: "BASS NATION", beats: 7954 },
    { rank: 4, name: "DROP ZONE", beats: 7232 },
    { rank: 5, name: "TRAP HOUSE", beats: 6901 },
    { rank: 6, name: "SYNTH WAVE", beats: 6487 },
    { rank: 7, name: "LOOP LEGENDS", beats: 6056 },
    { rank: 8, name: "SAMPLE KINGS", beats: 5723 },
    { rank: 9, name: "MIX MASTERS", beats: 5401 },
    { rank: 10, name: "AUDIO ALLIANCE", beats: 5189 },
    { rank: 11, name: "SOUND SQUAD", beats: 4876 },
    { rank: 12, name: "FREQ FAMILY", beats: 4543 },
    { rank: 13, name: "WAVE WARRIORS", beats: 4212 },
    { rank: 14, name: "TEMPO TRIBE", beats: 3901 },
    { rank: 15, name: "VIBE VILLAGE", beats: 3689 },
    { rank: 16, name: "GROOVE GANG", beats: 3454 },
    { rank: 17, name: "ECHO EMPIRE", beats: 3221 },
    { rank: 18, name: "PITCH PACK", beats: 3012 },
    { rank: 19, name: "BASS BRIGADE", beats: 2801 },
    { rank: 20, name: "DROP DYNASTY", beats: 2598 },
  ],
};

const getMedalIcon = (rank: number) => {
  if (rank === 1) return require('@/assets/icons/gold-medal.png');
  if (rank === 2) return require('@/assets/icons/silver-medal.png');
  if (rank === 3) return require('@/assets/icons/bronze-medal.png');
  return null;
};

export default function Leaderboard() {
  const [text, setText] = useState<string>('');
  const [selectedTab, setSelectedTab] = useState<TabName>('Global');

  const currentData = mockData[selectedTab] || [];

  const renderItem = ({ item }: { item: LeaderboardEntry }) => (
    <View 
      style={[
        styles.leaderboardItem,
        item.rank <= 3 && styles.topThreeItem
      ]}
    >
      {item.rank <= 3 ? (
        <Image source={getMedalIcon(item.rank)} style={styles.medalIcon} />
      ) : (
        <Text style={styles.rankNumber}>{item.rank}</Text>
      )}
      <Text style={styles.nameText}>{item.name}</Text>
      <Text style={styles.dotsText}>-------------------</Text>
      <Text style={styles.beatsText}>{item.beats} BEATS</Text>
    </View>
  );

  const handleLoadMore = () => {
    // TODO: Load more data when needed
    console.log('Load more data...');
  };

  return (
    <SafeAreaView style={containerStyles.container}>
      <Text style={objectStyles.title}>Leaderboard</Text>
      <Image
        source={require('@/assets/icons/leaderboard.png')}
        style={styles.leaderboardIcon}
      />
      <View style={containerStyles.innerBoxWrapper}>
        <View style={containerStyles.innerBox}>
          <View style={containerStyles.buttonRow}>
            <TouchableOpacity
              style={[
                styles.tabButton,
                selectedTab === 'Global' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setSelectedTab('Global')}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  selectedTab === 'Global' ? objectStyles.TextActive : objectStyles.TextInactive,
                ]}
              >
                Global
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.tabButton,
                selectedTab === 'Users' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setSelectedTab('Users')}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  selectedTab === 'Users' ? objectStyles.TextActive : objectStyles.TextInactive,
                ]}
              >
                Users
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.tabButton,
                selectedTab === 'Cliques' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
              ]}
              onPress={() => setSelectedTab('Cliques')}
            >
              <Text
                style={[
                  objectStyles.tabText,
                  selectedTab === 'Cliques' ? objectStyles.TextActive : objectStyles.TextInactive,
                ]}
              >
                Cliques
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

          <Text style={objectStyles.sectionTitle}>{selectedTab.toUpperCase()}</Text>

          <FlatList
            data={currentData}
            renderItem={renderItem}
            keyExtractor={(item) => item.rank.toString()}
            style={styles.flatList}
            showsVerticalScrollIndicator={false}
            onEndReached={handleLoadMore}
            onEndReachedThreshold={0.5}
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  searchIcon: {
    ...objectStyles.genericIcon,
    marginLeft: 10,
  },

  leaderboardIcon: {
    width: 90,
    height: 90,
    tintColor: clusterColors.clusterYellow,
    alignSelf: 'center',
  },

  tabButton: {
    flex: 1,
    marginHorizontal: 5,
    borderRadius: 9,
    paddingVertical: 10,
    alignItems: 'center',
  },
  
  flatList: {
    width: '100%',
    flex: 1,
  },
  leaderboardItem: {
    ...containerStyles.genericRow, 
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#3a3a3a',
  },

  topThreeItem: {
    backgroundColor: '#333',
  },

  medalIcon: {
    width: 30,
    height: 30,
    marginRight: 10,
  },

  rankNumber: {
    color: '#FFF',
    fontFamily: 'FuturaPT-Bold',
    fontSize: 14,
    width: 30,
    marginRight: 10,
  },

  nameText: {
    color: '#FFF',
    fontFamily: 'FuturaPT-Med',
    fontSize: 14,
    flex: 1,
  },
  dotsText: {
    color: '#555',
    fontSize: 12,
    marginHorizontal: 5,
  },

  beatsText: {
    color: '#FFF',
    fontFamily: 'FuturaPT-Bold',
    fontSize: 12,
  },
});
