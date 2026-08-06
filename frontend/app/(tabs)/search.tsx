import { useSession } from "@/components/auth";
import * as Routes from "@/components/routes";
import { clusterColors, containerStyles, objectStyles } from "@/constants/style";
import { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, FlatList, Image, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

interface ExploreItem {
 id: number;
 name: string;
 type: 'cluster' | 'clique' | 'user';
 image?: string;
 subtitle?: string;
}

export default function SearchPage() {
 const [text, setText] = useState("");
 const [showRecent, setShowRecent] = useState(true);
 const [selectedTab, setSelectedTab] = useState<'clusters' | 'cliques' | 'users'>('clusters');
 const [clusters, setClusters] = useState<ExploreItem[]>([]);
 const [cliques, setCliques] = useState<ExploreItem[]>([]);
 const [users, setUsers] = useState<ExploreItem[]>([]);
 const [isLoading, setIsLoading] = useState<boolean>(false);
 const [isSearching, setIsSearching] = useState<boolean>(false);
 const [error, setError] = useState<string | null>(null);
 const [hasSearched, setHasSearched] = useState<boolean>(false);
 const session = useSession();
 const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

 const recents = [
  "You can search for clusters",
  "You can also search for users",
  "Searching for cliques can be done here too",
  "Finally, search for a specific community for more information as well.",
 ];

 // Debounced search function
 const performSearch = useCallback(async (query: string) => {
  if (!query.trim()) {
   setClusters([]);
   setCliques([]);
   setUsers([]);
   setHasSearched(false);
   return;
  }

  try {
   setIsSearching(true);
   setError(null);
   
   const result = await Routes.search(session, query, 20);
   
   if (result.ok) {
    const searchData = result.data;
    
    setClusters(searchData.clusters.map(item => ({
     id: item.id,
     name: item.name,
     type: 'cluster' as const,
     image: item.image,
     subtitle: item.subtitle,
    })));
    
    setCliques(searchData.cliques.map(item => ({
     id: item.id,
     name: item.name,
     type: 'clique' as const,
     image: item.image,
     subtitle: item.subtitle,
    })));
    
    setUsers(searchData.users.map(item => ({
     id: item.id,
     name: item.name,
     type: 'user' as const,
     image: item.image,
     subtitle: item.subtitle,
    })));
    
    setHasSearched(true);
   } else {
    setError(result.error);
   }
  } catch (e) {
   setError((e as Error).message || 'Search failed');
  } finally {
   setIsSearching(false);
  }
 }, [session]);

 // Handle text change with debounce
 const handleTextChange = useCallback((newText: string) => {
  setText(newText);
  
  // Clear existing timeout
  if (searchTimeoutRef.current) {
   clearTimeout(searchTimeoutRef.current);
  }
  
  // Set new timeout for debounced search
  searchTimeoutRef.current = setTimeout(() => {
   performSearch(newText);
  }, 300); // 300ms debounce
 }, [performSearch]);

 // Cleanup timeout on unmount
 useEffect(() => {
  return () => {
   if (searchTimeoutRef.current) {
    clearTimeout(searchTimeoutRef.current);
   }
  };
 }, []);

 // Handle search button press
 const handleSearchPress = useCallback(() => {
  if (searchTimeoutRef.current) {
   clearTimeout(searchTimeoutRef.current);
  }
  performSearch(text);
 }, [text, performSearch]);

 const getCurrentData = () => {
  switch (selectedTab) {
   case 'clusters': return clusters;
   case 'cliques': return cliques;
   case 'users': return users;
   default: return [];
  }
 };
 
 const handleItemPress = (item: ExploreItem) => {
  if (item.type === 'user') {
   //router.push(`/(tabs)/${item.id}`);
  } else if (item.type === 'clique') {
   // nav to clique page
   console.log(`Navigate to clique: ${item.id}`);
  } else if (item.type === 'cluster') {
   // nav to cluster page
   console.log(`Navigate to cluster: ${item.id}`);
  }
 };

 const renderItem = ({ item }: { item: ExploreItem }) => (
  <TouchableOpacity
   style={styles.box}
   onPress={() => handleItemPress(item)}
  >
   <View style={containerStyles.genericRow}>
    {item.image ? (
     <Image
      source={{ uri: item.image }}
      style={styles.profileIcon}
     />
    ) : (
     <Image
      source={require('@/assets/icons/profile.png')}
      style={styles.icon}
     />
    )}
    <View style={styles.textWrapper}>
     <Text style={objectStyles.subsectionTitle}>{item.name}</Text>
     {item.subtitle && <Text style={styles.subtitleText}>{item.subtitle}</Text>}
    </View>
   </View>
  </TouchableOpacity>
 );

 return (
  <SafeAreaView style={containerStyles.container}>
   <Text style={objectStyles.title}>Explore</Text>

   <View style={containerStyles.searchRow}>
    <TextInput
     style={objectStyles.textBox}
     value={text}
     onChangeText={handleTextChange}
     placeholder="Search for clusters, cliques, or users..."
     placeholderTextColor="#777"
     returnKeyType="search"
     onSubmitEditing={handleSearchPress}
    />
    <TouchableOpacity onPress={handleSearchPress}>
     {isSearching ? (
      <ActivityIndicator size="small" color={clusterColors.clusterYellow} style={styles.searchIcon} />
     ) : (
      <Image
       source={require("@/assets/icons/search.png")}
       style={styles.searchIcon}
      />
     )}
    </TouchableOpacity>
   </View>

   {showRecent && !hasSearched && !text.trim() && (
    <View style={styles.recentContainer}>
     {recents.map((recent, index) => (
      <View key={index} style={styles.recentRow}>
       <Image
        source={require("@/assets/icons/refresh.png")}
        style={styles.recentIcon}
       />
       <Text style={styles.recentText}>{recent}</Text>
       <TouchableOpacity
        onPress={() => {
         const newTips = recents.filter((_, i) => i !== index);
         if (newTips.length === 0) setShowRecent(false);
        }}
       >
        <Image
         source={require("@/assets/icons/x.png")}
         style={styles.closeIcon}
        />
       </TouchableOpacity>
      </View>
     ))}
    </View>
   )}

   <View style={containerStyles.innerBoxWrapper}>
    <View style={containerStyles.innerBox}>
     <View style={containerStyles.buttonRow}>
      <TouchableOpacity
       style={[
        styles.tabButton,
        selectedTab === 'clusters' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
       ]}
       onPress={() => setSelectedTab('clusters')}
      >
       <Text
        style={[
         objectStyles.tabText,
         selectedTab === 'clusters' ? objectStyles.TextActive : objectStyles.TextInactive,
        ]}
       >
        Clusters
       </Text>
      </TouchableOpacity>

      <TouchableOpacity
       style={[
        styles.tabButton,
        selectedTab === 'cliques' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
       ]}
       onPress={() => setSelectedTab('cliques')}
      >
       <Text
        style={[
         objectStyles.tabText,
         selectedTab === 'cliques' ? objectStyles.TextActive : objectStyles.TextInactive,
        ]}
       >
        Cliques
       </Text>
      </TouchableOpacity>

      <TouchableOpacity
       style={[
        styles.tabButton,
        selectedTab === 'users' ? objectStyles.tabButtonActive : objectStyles.tabButtonInactive,
       ]}
       onPress={() => setSelectedTab('users')}
      >
       <Text
        style={[
         objectStyles.tabText,
         selectedTab === 'users' ? objectStyles.TextActive : objectStyles.TextInactive,
        ]}
       >
        Users
       </Text>
      </TouchableOpacity>
     </View>

     <FlatList
      data={getCurrentData()}
      renderItem={renderItem}
      keyExtractor={(item) => `${item.type}-${item.id}`}
      style={styles.flatList}
      showsVerticalScrollIndicator={false}
      ListEmptyComponent={
       <View style={styles.emptyContainer}>
        {isSearching ? (
         <ActivityIndicator size="large" color={clusterColors.clusterYellow} />
        ) : (
         <Text style={styles.emptyText}>
          {!text.trim() 
           ? `Start typing to search for ${selectedTab}` 
           : hasSearched 
             ? `No ${selectedTab} found for "${text}"`
             : `Searching...`
          }
         </Text>
        )}
       </View>
      }
     />
    </View>
   </View>
  </SafeAreaView>
 );
}

const styles = StyleSheet.create({
 centerContent: {
  justifyContent: 'center',
  alignItems: 'center',
 },
 searchIcon: {
  ...objectStyles.genericIcon,
  marginLeft: 10,
 },

 recentContainer: {
  width: "92%",
  alignSelf: "center",
  borderRadius: 12,
  padding: 15,
  paddingTop: 5,
  paddingBottom: 5,
  borderBottomWidth: 1,
  borderColor: clusterColors.clusterTint,
 },

 recentRow: {
  ...containerStyles.genericRow,
  flex: 0,
  marginBottom: 12,
 },

 recentIcon: {
  ...objectStyles.genericIcon,
  width: 20,
  height: 20,
  marginRight: 10,
 },
 recentText: {
  flex: 1,
  color: clusterColors.dimGray,
  fontFamily: "FuturaPT-Book",
  fontSize: 14,
 },

 closeIcon: {
  ...objectStyles.genericIcon,
  width: 18,
  height: 18,
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
 box: {
  ...containerStyles.genericRow, 
  paddingVertical: 8,
  paddingHorizontal: 10,
  borderBottomWidth: 1,
  borderBottomColor: '#3a3a3a',
 },
 icon: {
  ...objectStyles.genericIcon,
  width: 35,
  height: 35,
  marginRight: 10,
 },
 profileIcon: {
  width: 35,
  height: 35,
  borderRadius: 17.5,
  marginRight: 10,
 },
 textWrapper: {
  flexDirection: 'column',
  flex: 1,
 },
 subtitleText: {
  color: clusterColors.dimGray,
  fontSize: 12,
  fontFamily: 'FuturaPT-Light',
  marginTop: 2,
 },
 emptyText: {
  color: clusterColors.dimGray,
  fontSize: 16,
  fontFamily: 'FuturaPT-Book',
  textAlign: 'center',
  padding: 20,
 },
 emptyContainer: {
  flex: 1,
  justifyContent: 'center',
  alignItems: 'center',
  paddingVertical: 40,
 },
});
