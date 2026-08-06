import { clusterColors, objectStyles } from "@/constants/style";
import Slider from "@react-native-community/slider";
import { useEffect, useRef, useState } from "react";
import {
  Animated,
  Easing,
  LayoutChangeEvent,
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from "react-native";
import TextTicker from 'react-native-text-ticker';

export default function MusicBar() {
  const [title, setTitle] = useState("Runaway - Kanye West ft. Pusha TRunaway - Kanye West ft. Pusha TRunaway - Kanye West ft. Pusha T");
  const [isPlaying, setIsPlaying] = useState(true);
  const [progress, setProgress] = useState(0);

  const spinValue = useRef(new Animated.Value(0)).current;
  const spinAnimRef = useRef<Animated.CompositeAnimation | null>(null);

  const titleButton = () => {};

  useEffect(() => {
    if (spinAnimRef.current) {
      spinAnimRef.current.stop();
    }
    
    spinValue.setValue(0);

    if (isPlaying) {
      spinAnimRef.current = Animated.loop(
        Animated.timing(spinValue, {
          toValue: 1,
          duration: 4000,
          easing: Easing.linear,
          useNativeDriver: true,
        })
      );
      spinAnimRef.current.start();
    }
  }, [isPlaying]);

  const spin = spinValue.interpolate({
    inputRange: [0, 1],
    outputRange: ["0deg", "360deg"],
  });

  const [textWidth, setTextWidth] = useState(0);
  const [containerWidth, setContainerWidth] = useState(0);
  const [measured, setMeasured] = useState(false);

  const onTextLayout = (e: LayoutChangeEvent) => {
    setTextWidth(e.nativeEvent.layout.width);
    setMeasured(true);
  };
  
  const onContainerLayout = (e: LayoutChangeEvent) => {
    setContainerWidth(e.nativeEvent.layout.width);
  };

  useEffect(() => {
    setMeasured(false);
    setTextWidth(0);
  }, [title]);

  const needsScroll = measured && textWidth > containerWidth;

  return (
    <View style={styles.playBar}>
      <TouchableOpacity onPress={() => setIsPlaying((prev) => !prev)}>
        <Animated.Image
          source={require("@/assets/icons/vinyl.png")}
          style={[
            styles.vinylIcon,
            { transform: [{ rotate: spin }] },
            isPlaying ? {tintColor: clusterColors.clusterYellow} : {tintColor: clusterColors.clusterTint}
          ]}
        />
      </TouchableOpacity>

      <View style={styles.centerSection}>
        
        <TouchableOpacity 
          style={styles.textClipper} 
          onLayout={onContainerLayout}
          onPress={titleButton}
          activeOpacity={0.7}
        >
          {needsScroll ? (
            <TextTicker 
              style={styles.playBarText}
              duration={20000}
              loop
              bounce={false}
              repeatSpacer={50}
              marqueeDelay={0}
            >
              {title}
            </TextTicker>
          ) : (
            <Text 
              style={styles.playBarText}
              onLayout={onTextLayout}
              numberOfLines={1}
            >
              {title}
            </Text>
          )}
        </TouchableOpacity>
        <Slider
          style={styles.slider}
          value={progress}
          minimumTrackTintColor={clusterColors.clusterTint}
          maximumTrackTintColor={clusterColors.clusterTint}
          thumbTintColor={clusterColors.clusterTint}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  playBar: {
    height: 60,
    flexDirection: "row",
    paddingHorizontal: 15,
    backgroundColor: clusterColors.clusterBlue,
    justifyContent: "center",
    alignItems: "center",
  },
  vinylIcon: {
    ...objectStyles.genericIcon,
    width: 50,
    height: 50,
  },
  centerSection: {
    flex: 1,
    alignItems: "center",
  },
  playBarText: {
    color: clusterColors.clusterTint,
    fontFamily: "FuturaPT-Book",
    fontSize: 14,
    fontWeight: "600",
  },
  slider: {
    width: "100%",
    height: 30,
  },
  textClipper: {
    width: "90%",
    height: 20,
    overflow: "hidden",
    justifyContent: "center",
  },
});
