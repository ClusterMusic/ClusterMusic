import { useSession } from "@/components/auth";
import * as Routes from "@/components/routes";
import { clusterColors, containerStyles, objectStyles } from "@/constants/style";
import { AccountSettings, NotificationSettings, PlayerSettings, PrivacySettings, SettingsContext, UISettings } from "@/context/SettingsContext";
import Slider from "@react-native-community/slider";
import { Picker } from '@react-native-picker/picker';
import * as Location from "expo-location";
import React, { useCallback, useContext, useEffect, useState } from "react";
import { ActivityIndicator, Alert, FlatList, Image, StyleSheet, Switch, Text, TextInput, TouchableOpacity, View } from "react-native";
import Collapsible from "react-native-collapsible";
import { SafeAreaView } from "react-native-safe-area-context";

export default function SettingsPage() {
  const settings = useContext(SettingsContext);
  const session = useSession();
  const [savingSection, setSavingSection] = useState<string | null>(null);
  const [isLoadingSettings, setIsLoadingSettings] = useState(true);
  
  const sections = Object.entries(settings).map(([key, value]) => ({
    key,
    name: key.charAt(0).toUpperCase() + key.slice(1),
    message: "Tap to expand settings",
    icon: require("@/assets/icons/profile.png"),
    section: value,
  }));

  const [openSections, setOpenSections] = useState<{ [key: string]: boolean }>({});

  // Load settings from backend on mount
  const loadSettings = useCallback(async () => {
    try {
      setIsLoadingSettings(true);
      const result = await Routes.getSettings(session);
      
      if (result.ok) {
        const serverSettings = result.data;
        
        // Update local settings with server data
        settings.privacy.setValue(serverSettings.privacy);
        settings.notifications.setValue(serverSettings.notifications);
        settings.ui.setValue(serverSettings.ui);
        settings.player.setValue({
          defaultService: serverSettings.player.defaultService as 'spotify' | 'appleMusic'
        });
        
        if (serverSettings.location) {
          settings.account.setValue((prev: AccountSettings) => ({
            ...prev,
            location: serverSettings.location
          }));
        }
      }
    } catch (error) {
      console.warn('Failed to load settings from server:', error);
    } finally {
      setIsLoadingSettings(false);
    }
  }, [session, settings]);

  useEffect(() => {
    loadSettings();
  }, []);

  const toggleSection = (key: string) => {
    setOpenSections((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  // Save settings to backend
  const saveSection = async (key: string) => {
    setSavingSection(key);
    
    try {
      if (key === 'account') {
        // Update profile (displayName, biography)
        const accountValue = settings.account.value;
        const profileResult = await Routes.updateProfile(session, {
          displayName: accountValue.displayName || undefined,
          biography: accountValue.biography || undefined,
          image: accountValue.profilePicture || undefined,
        });
        
        if (!profileResult.ok) {
          Alert.alert('Error', profileResult.error || 'Failed to update profile');
          setSavingSection(null); // Reset before early return
          return;
        }
        
        // Also update location in settings if present
        if (accountValue.location) {
          const settingsResult = await Routes.updateSettings(session, {
            location: {
              latitude: accountValue.location.latitude,
              longitude: accountValue.location.longitude,
              city: accountValue.location.city,
              region: accountValue.location.region,
            }
          });
          
          if (!settingsResult.ok) {
            console.warn('Failed to update location settings:', settingsResult.error);
          }
        }
        
        Alert.alert('Success', 'Account settings saved successfully');
      } else {
        // Build settings update request based on which section is being saved
        const updateRequest: Routes.UpdateSettingsRequest = {};
        
        if (key === 'privacy') {
          updateRequest.privacy = settings.privacy.value as PrivacySettings;
        } else if (key === 'notifications') {
          updateRequest.notifications = settings.notifications.value as NotificationSettings;
        } else if (key === 'ui') {
          updateRequest.ui = settings.ui.value as UISettings;
        } else if (key === 'player') {
          updateRequest.player = {
            defaultService: settings.player.value.defaultService
          };
        }
        
        const result = await Routes.updateSettings(session, updateRequest);
        
        if (result.ok) {
          Alert.alert('Success', `${key.charAt(0).toUpperCase() + key.slice(1)} settings saved successfully`);
        } else {
          Alert.alert('Error', result.error || 'Failed to save settings');
        }
      }
    } catch (error) {
      console.error('Error saving settings:', error);
      Alert.alert('Error', 'An unexpected error occurred while saving settings');
    } finally {
      setSavingSection(null);
    }
  };

  const reconfigureLocation = async (item: typeof sections[0]) => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") {
        alert("Location permission denied");
        return;
      }

      const loc = await Location.getCurrentPositionAsync({});
      const address = await Location.reverseGeocodeAsync({
        latitude: loc.coords.latitude,
        longitude: loc.coords.longitude,
      });

      const locationData = {
        latitude: loc.coords.latitude,
        longitude: loc.coords.longitude,
        city: address[0]?.city ?? null,
        region: address[0]?.region ?? null,
      };

      item.section.setValue((prev: any) => ({
        ...prev,
        location: locationData,
      }));

      alert("Location updated!");
    } catch (err) {
      console.warn("Failed to fetch location:", err);
      alert("Failed to update location");
    }
  };

  const renderField = (item: typeof sections[0], fieldKey: string, value: any) => {
    if (fieldKey === "location") {
      return (
        <View key={fieldKey} style={styles.fieldColumn}>
          <Text style={objectStyles.subsectionTitle}>Location</Text>
          {value && (
            <Text style={styles.locationText}>
              {value.city}, {value.region}
            </Text>
          )}
          <TouchableOpacity
            style={[styles.tabButton, objectStyles.tabButtonActive, { marginVertical: 5 }]}
            onPress={() => reconfigureLocation(item)}
          >
            <Text style={objectStyles.TextActive}>Reconfigure Location</Text>
          </TouchableOpacity>
        </View>
      );
    }

    if (value === null) return null;

    if (typeof value === "boolean") {
      return (
        <View key={fieldKey} style={styles.fieldRow}>
          <Text style={objectStyles.subsectionTitle}>{fieldKey}</Text>
          <Switch
            value={item.section.value[fieldKey]}
            onValueChange={(val) =>
              item.section.setValue((prev: any) => ({
                ...prev,
                [fieldKey]: val,
              }))
            }
          />
        </View>
      );
    } else if (typeof value === "number") {
      return (
        <View key={fieldKey} style={styles.fieldColumn}>
          <Text style={objectStyles.subsectionTitle}>{fieldKey}: {item.section.value[fieldKey]}</Text>
          <Slider
            style={styles.slider}
            minimumValue={0}
            maximumValue={100}
            step={1}
            value={item.section.value[fieldKey]}
            onValueChange={(val) =>
              item.section.setValue((prev: any) => ({
                ...prev,
                [fieldKey]: val,
              }))
            }
          />
        </View>
      );
    } else if (typeof value === "string") {
      if (fieldKey === "language") {
        return (
          <View key={fieldKey} style={styles.fieldColumn}>
            <Text style={objectStyles.subsectionTitle}>{fieldKey}</Text>
            <Picker
              selectedValue={item.section.value[fieldKey]}
              style={styles.picker}
              onValueChange={(val) =>
                item.section.setValue((prev: any) => ({
                  ...prev,
                  [fieldKey]: val,
                }))
              }
            >
              <Picker.Item label="English" value="en" />
              <Picker.Item label="Spanish" value="es" />
              <Picker.Item label="French" value="fr" />
              <Picker.Item label="German" value="de" />
            </Picker>
          </View>
        );
      }

      return (
        <View key={fieldKey} style={styles.fieldColumn}>
          <Text style={objectStyles.subsectionTitle}>{fieldKey}</Text>
          <TextInput
            style={[
              objectStyles.textBox,
              fieldKey === "biography" && styles.textInputMultiline,
            ]}
            value={item.section.value[fieldKey]}
            onChangeText={(text) =>
              item.section.setValue((prev: any) => ({
                ...prev,
                [fieldKey]: text,
              }))
            }
            placeholder={`Enter ${fieldKey}`}
            placeholderTextColor="#666"
            multiline={fieldKey === "biography"}
            numberOfLines={fieldKey === "biography" ? 4 : 1}
          />
        </View>
      );
    }
    return null;
  };

  const renderItem = ({ item }: { item: typeof sections[0] }) => {
    const isOpen = !!openSections[item.key];
    
    return (
      <View>
        <TouchableOpacity activeOpacity={0.8} onPress={() => toggleSection(item.key)}>
          <View style={[styles.activeBox, isOpen && { backgroundColor: "#444" }]}>
            <View style={containerStyles.genericRow}>
              <Image source={item.icon} style={styles.icon} />
              <View>
                <Text style={objectStyles.subsectionTitle}>{item.name}</Text>
                <Text style={styles.boxSubtitle}>{item.message}</Text>
              </View>
            </View>
          </View>
        </TouchableOpacity>

        <Collapsible collapsed={!isOpen}>
          <View style={styles.contentBox}>
            {Object.entries(item.section.value).map(([fieldKey, value]) =>
              renderField(item, fieldKey, value)
            )}

            <TouchableOpacity
              style={[styles.tabButton, objectStyles.tabButtonActive]}
              onPress={() => saveSection(item.key)}
              disabled={savingSection === item.key}
            >
              {savingSection === item.key ? (
                <ActivityIndicator size="small" color={clusterColors.fullBlack} />
              ) : (
                <Text style={objectStyles.TextActive}>Save</Text>
              )}
            </TouchableOpacity>
          </View>
        </Collapsible>
      </View>
    );
  };

  if (isLoadingSettings) {
    return (
      <SafeAreaView style={[containerStyles.container, styles.loadingContainer]}>
        <ActivityIndicator size="large" color={clusterColors.clusterYellow} />
        <Text style={styles.loadingText}>Loading settings...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={containerStyles.container}>
      <Text style={[objectStyles.title, styles.pageTitle]}>Settings</Text>
      <FlatList
        data={sections}
        keyExtractor={(item) => item.key}
        renderItem={renderItem}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  activeBox: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: "#3a3a3a",
    backgroundColor: "#333",
  },
  icon: {
    ...objectStyles.genericIcon,
    width: 35,
    height: 35,
    marginRight: 10,
  },
  textWrapper: {
    flexDirection: "column",
    flex: 1,
  },
  boxSubtitle: {
    ...objectStyles.lowerOpacity,
    fontSize: 13,
    marginTop: 2,
  },
  contentBox: {
    padding: 12,
    backgroundColor: clusterColors.innerGray,
    borderBottomWidth: 1,
    borderBottomColor: "#3a3a3a",
  },
  fieldRow: {
    ...containerStyles.genericRow,
    justifyContent: "space-between",
    marginVertical: 8,
  },
  fieldColumn: {
    flexDirection: "column",
    marginVertical: 8,
  },
  fieldLabel: {
    color: "#fff",
    fontSize: 14,
    marginBottom: 4,
  },
  locationText: {
    ...objectStyles.subsectionTitle,
    color: clusterColors.dimGray,
    fontSize: 14,
    marginBottom: 8,
  },
  textInputMultiline: {
    height: 80,
    textAlignVertical: "top",
  },
  slider: {
    width: "100%",
    height: 40,
  },
  picker: {
    color: clusterColors.fullWhite,
    backgroundColor: clusterColors.innerGray,
  },
  tabButton: {
    flex: 1,
    marginVertical: 10,
    marginHorizontal: 5,
    borderRadius: 9,
    paddingVertical: 10,
    alignItems: "center",
  },
  loadingContainer: {
    justifyContent: "center",
    alignItems: "center",
  },
  loadingText: {
    color: clusterColors.dimGray,
    fontSize: 16,
    fontFamily: "FuturaPT-Book",
    marginTop: 12,
  },
  pageTitle: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
});
