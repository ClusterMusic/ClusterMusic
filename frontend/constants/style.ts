import { StyleSheet } from "react-native";



export const clusterColors = {
  fullWhite: "#FFF",
  fullBlack: "#000",
  warmBlack: "#0F1010",
  dimGray: "#CCC",
  innerGray: "#2a2a2a",
  clusterBlue: "#086788",
  clusterTint: "#FFE9C9",
  clusterYellow: "#EFA00B",

}

export const containerStyles = StyleSheet.create({
  container: {
    flex: 1,
  },
  innerBoxWrapper: {
    flex: 1,
    padding: 10,
    paddingTop: 10,
    paddingBottom: 0,
  },
  innerBox: {
    flex: 1,
    backgroundColor: clusterColors.innerGray,
    borderRadius: 12,
    padding: 15,
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  genericRow: {
    flexDirection: 'row',
    alignItems: 'center',
    //flex: 1,
  },

  buttonRow: {
    marginVertical: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '85%',
  },
  searchRow: {
    flexDirection: "row",
    alignItems: "center",
    alignSelf: "center",
    width: "90%",
    marginBottom: 15,
  },

});

export const objectStyles = StyleSheet.create({
  title: {
    color: clusterColors.fullWhite,
    fontFamily: "FuturaPT-Bold",
    fontWeight: 'bold',
    textAlign: 'center',
    margin: 5,
    fontSize: 24,
    textShadowColor: clusterColors.clusterBlue,
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 1,
    padding: 5,
  },
  sectionTitle: {
    padding: 10,
    color: clusterColors.fullWhite,
    textAlign: 'center',
    fontFamily: 'FuturaPT-Bold',
    fontSize: 18,
    marginBottom: 5,
  },
  subsectionTitle:{
    color: clusterColors.fullWhite,
    fontSize: 15,
    fontFamily: 'FuturaPT-Book',
  },
  textBox: {
    flex: 1,
    borderRadius: 9,
    backgroundColor: clusterColors.warmBlack,
    fontFamily: 'FuturaPT-Book',
    color: '#fff',
    paddingVertical: 8,
    paddingHorizontal: 15,
    fontSize: 16,
  },
  tabButtonActive: {
    backgroundColor: clusterColors.clusterYellow,
  },
  tabButtonInactive: {
    backgroundColor: clusterColors.clusterTint,
  },
  TextActive: {
    color: clusterColors.fullWhite,
    fontFamily: 'FuturaPT-Bold',
  },
  TextInactive: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Med',
  },
  tabText: {
    textAlign: 'center',
    fontFamily: "FuturaPT-Med",
    fontSize: 13,
    fontStyle: 'normal',
    lineHeight: 16,
  },
  genericIcon: {
    width: 24,
    height: 24,
    tintColor: clusterColors.clusterTint, 
  },
  lowerOpacity: {
    color: '#CCC',
    fontFamily: 'FuturaPT-Light',
  },
  smallText: {
    color: clusterColors.fullBlack,
    fontFamily: 'FuturaPT-Book',
  },

})
