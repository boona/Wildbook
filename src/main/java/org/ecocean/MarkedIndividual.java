/*
 * Wildbook - A Mark-Recapture Framework
 * Copyright (C) 2011-2013 Jason Holmberg
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.ecocean;

import java.io.IOException;
import java.util.*;

import org.ecocean.genetics.*;
import org.ecocean.social.Relationship;

import java.text.DecimalFormat;

/**
 * A <code>MarkedIndividual</code> object stores the complete <code>encounter</code> data for a single marked individual in a mark-recapture study.
 * <code>Encounters</code> are added to MarkedIndividual objects as multiple encounters are associated with
 * known marked individuals.
 * <p/>
 *
 * @author Jason Holmberg
 * @version 2.0
 * @see Encounter, Shepherd
 */
public class MarkedIndividual implements java.io.Serializable {

  //unique name of the MarkedIndividual, such as 'A-109'
  private String individualID = "";

  //alternate id for the MarkedIndividual, such as a physical tag number of reference in another database
  private String alternateid;

  //additional comments added by researchers
  private String comments = "None";

  //sex of the MarkedIndividual
  private String sex = "unknown";

  //unused String that allows groups of MarkedIndividuals by optional parameters
  private String seriesCode = "None";

  //nickname for the MarkedIndividual...not used for any scientific purpose
  //also the nicknamer for credit
  private String nickName = "", nickNamer = "";

  //Vector of approved encounter objects added to this MarkedIndividual
  private Vector encounters = new Vector();

  //Vector of unapproved encounter objects added to this MarkedIndividual
  private Vector unidentifiableEncounters = new Vector();

  //Vector of String filenames of additional files added to the MarkedIndividual
  private Vector dataFiles = new Vector();

  //number of encounters of this MarkedIndividual
  private int numberEncounters;

  //number of unapproved encounters (log) of this MarkedIndividual
  private int numUnidentifiableEncounters;

  //a Vector of Strings of email addresses to notify when this MarkedIndividual is modified
  private Vector interestedResearchers = new Vector();

  private String dateTimeCreated;
  
  //FOR FAST QUERY PURPOSES ONLY - DO NOT MANUALLY SET
  private String localHaplotypeReflection;

  private String dynamicProperties;

  private String patterningCode;

  private int maxYearsBetweenResightings;
  
  private long timeOfBirth=0;
  
  private long timeOfDeath=0;

  public MarkedIndividual(String individualID, Encounter enc) {

    this.individualID = individualID;
    encounters.add(enc);
    //dataFiles = new Vector();
    numberEncounters = 1;
    this.sex = enc.getSex();
    numUnidentifiableEncounters = 0;
    maxYearsBetweenResightings=0;
  }

  /**
   * empty constructor used by JDO Enhancer - DO NOT USE
   */
  public MarkedIndividual() {
  }


  /**Adds a new encounter to this MarkedIndividual.
   *@param  newEncounter  the new <code>encounter</code> to add
   *@return true for successful addition, false for unsuccessful - Note: this change must still be committed for it to be stored in the database
   *@see  Shepherd#commitDBTransaction()
   */

  public boolean addEncounter(Encounter newEncounter) {

      newEncounter.assignToMarkedIndividual(individualID);
   
      //get and therefore set the haplotype if necessary
      getHaplotype();
      
      boolean ok=encounters.add(newEncounter);
      numberEncounters++;
      resetMaxNumYearsBetweenSightings();
      return ok; 
     
 }

   /**Removes an encounter from this MarkedIndividual.
   *@param  getRidOfMe  the <code>encounter</code> to remove from this MarkedIndividual
   *@return true for successful removal, false for unsuccessful - Note: this change must still be committed for it to be stored in the database
   *@see  Shepherd#commitDBTransaction()
   */
  public boolean removeEncounter(Encounter getRidOfMe){

      numberEncounters--;
      
      
      
      boolean changed=false;
      for(int i=0;i<encounters.size();i++) {
        Encounter tempEnc=(Encounter)encounters.get(i);
        if(tempEnc.getEncounterNumber().equals(getRidOfMe.getEncounterNumber())) {
          encounters.remove(i);
          i--;
          changed=true;
          }
        }
      resetMaxNumYearsBetweenSightings();
      
      //reset haplotype
      localHaplotypeReflection=null;
      getHaplotype();
      
      return changed;
  }
  

  /**
   * Returns the total number of submitted encounters for this MarkedIndividual
   *
   * @return the total number of encounters recorded for this MarkedIndividual
   */
  public int totalEncounters() {
    return encounters.size();
  }

  public int totalLogEncounters() {
    if (unidentifiableEncounters == null) {
      unidentifiableEncounters = new Vector();
    }
    return unidentifiableEncounters.size();
  }

  public Vector returnEncountersWithGPSData(){
    return returnEncountersWithGPSData(false,false);
  }
  public Vector returnEncountersWithGPSData(boolean useLocales, boolean reverseOrder) {
    //if(unidentifiableEncounters==null) {unidentifiableEncounters=new Vector();}
    Vector haveData=new Vector();
    Encounter[] myEncs=getDateSortedEncounters(reverseOrder);
    
    Properties localesProps = new Properties();
    if(useLocales){
      try {
        //localesProps.load(ShepherdPMF.class.getResourceAsStream("/bundles/locales.properties"));
        localesProps=ShepherdProperties.getProperties("locales.properties", "");
      } 
      catch (Exception ioe) {
        ioe.printStackTrace();
      }
    }
    
    for(int c=0;c<myEncs.length;c++) {
      Encounter temp=myEncs[c];
      if((temp.getDWCDecimalLatitude()!=null)&&(temp.getDWCDecimalLongitude()!=null)) {
        haveData.add(temp);
      }
      else if(useLocales && (temp.getLocationID()!=null) && (localesProps.getProperty(temp.getLocationID())!=null)){
        haveData.add(temp); 
      }

      }

    return haveData;

  }

  public boolean isDeceased() {
    if (unidentifiableEncounters == null) {
      unidentifiableEncounters = new Vector();
    }
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if (temp.getLivingStatus().equals("dead")) {
        return true;
      }
    }
    for (int d = 0; d < numUnidentifiableEncounters; d++) {
      Encounter temp = (Encounter) unidentifiableEncounters.get(d);
      if (temp.getLivingStatus().equals("dead")) {
        return true;
      }
    }
    return false;
  }

  public boolean wasSightedInYear(int year) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if (temp.getYear() == year) {
        return true;
      }
    }
    return false;
  }

  public boolean wasSightedInYear(int year, String locCode) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() == year) && (temp.getLocationCode().startsWith(locCode))) {
        return true;
      }
    }
    return false;
  }

  public boolean wasSightedInYearLeftTagsOnly(int year, String locCode) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() == year) && (temp.getLocationCode().startsWith(locCode)) && (temp.getNumSpots() > 0)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * 
   * @deprecated
   */
  public double averageLengthInYear(int year) {
    int numLengths = 0;
    double total = 0;
    double avg = 0;
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() == year) && ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0))) {
        total += temp.getSize();
        numLengths++;
      }
    }
    if (numLengths > 0) {
      avg = total / numLengths;
    }
    return avg;
  }
  
  
  /**
   * 
   * 
   * @deprecated
   */
  public double averageMeasuredLengthInYear(int year, boolean allowGuideGuess) {
    int numLengths = 0;
    double total = 0;
    double avg = 0;
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if (temp.getYear() == year) {
        if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0)) {
          if ((temp.getSizeGuess().equals("directly measured")) || ((allowGuideGuess) && (temp.getSizeGuess().equals("guide/researcher's guess")))) {

            total += temp.getSize();
            numLengths++;
          }
        }
      }
    }
    if (numLengths > 0) {
      avg = total / numLengths;
    }
    return avg;
  }

  //use the index identifier, not the full name of the keyword
  public boolean isDescribedByPhotoKeyword(Keyword word) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if(temp.hasKeyword(word)){return true;}
    }
    return false;
  }

  /*
  public boolean hasApprovedEncounters() {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if (temp.getState()!=null) {
        return true;
      }
    }
    return false;
  }
  */

  public boolean wasSightedInMonth(int year, int month) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() == year) && (temp.getMonth() == month)) {
        return true;
      }
    }
    return false;
  }


  public boolean wasSightedInPeriod(int m_startYear, int m_startMonth, int m_endYear, int m_endMonth) {
    int endYear = m_endYear;
    int endMonth = m_endMonth;

    int startYear = m_startYear;
    int startMonth = m_startMonth;


    GregorianCalendar gcMin=new GregorianCalendar(startYear, startMonth, 1);
    GregorianCalendar gcMax=new GregorianCalendar(endYear, endMonth, 31);



    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);

        if((temp.getDateInMilliseconds()>=gcMin.getTimeInMillis())&&(temp.getDateInMilliseconds()<=gcMax.getTimeInMillis())){
          return true;
        }
    }
    return false;
  }

  public boolean wasSightedInPeriod(int m_startYear, int m_startMonth, int m_startDay, int m_endYear, int m_endMonth, int m_endDay, String locCode) {
    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int endDay = m_endDay;
    int startYear = m_startYear;
    int startMonth = m_startMonth;
    int startDay = m_startDay;

    GregorianCalendar gcMin=new GregorianCalendar(startYear, startMonth, startDay);
    GregorianCalendar gcMax=new GregorianCalendar(endYear, endMonth, endDay);



    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);

      if ((temp.getLocationID()!=null)&&(!temp.getLocationID().trim().equals(""))&&(temp.getLocationID().trim().equals(locCode))) {

        if((temp.getDateInMilliseconds()>=gcMin.getTimeInMillis())&&(temp.getDateInMilliseconds()<=gcMax.getTimeInMillis())){
          return true;
        }
      }
    }
    return false;
  }

  public boolean wasSightedInPeriod(int m_startYear, int m_startMonth, int m_startDay, int m_endYear, int m_endMonth, int m_endDay) {
    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int endDay = m_endDay;
    int startYear = m_startYear;
    int startMonth = m_startMonth;
    int startDay = m_startDay;
    GregorianCalendar gcMin=new GregorianCalendar(startYear, startMonth, startDay);
    GregorianCalendar gcMax=new GregorianCalendar(endYear, endMonth, endDay);
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if((temp.getDateInMilliseconds()>=gcMin.getTimeInMillis())&&(temp.getDateInMilliseconds()<=gcMax.getTimeInMillis())){
          return true;
      }
    }
    return false;
  }

  public boolean wasSightedInPeriodLeftOnly(int m_startYear, int m_startMonth, int m_endYear, int m_endMonth) {
    int endYear = m_endYear;
    int endMonth = m_endMonth;

    int startYear = m_startYear;
    int startMonth = m_startMonth;

    GregorianCalendar gcMin=new GregorianCalendar(startYear, startMonth, 1);
    GregorianCalendar gcMax=new GregorianCalendar(endYear, endMonth, 31);



    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);

        if((temp.getDateInMilliseconds()>=gcMin.getTimeInMillis())&&(temp.getDateInMilliseconds()<=gcMax.getTimeInMillis())&&(temp.getNumSpots()>0)){
          return true;
        }
    }
    return false;
  }


  /**
   * Returns the user-input name of the MarkedIndividual, which is also used as an Index in the FastObjects database
   *
   * @return the name of the MarkedIndividual as a String
   */
  public String getName() {
    return individualID;
  }

  public String getIndividualID() {
      return individualID;
  }

  public String getNickName() {
    if (nickName != null) {
      return nickName;
    } else {
      return "Unassigned";
    }
  }

  public String getNickNamer() {
    if (nickNamer != null) {
      return nickNamer;
    } else {
      return "Unknown";
    }
  }

  /**
   * Sets the nickname of the MarkedIndividual.
   */
  public void setNickName(String newName) {
    nickName = newName;
  }

  public void setNickNamer(String newNamer) {
    nickNamer = newNamer;
  }

  public void setName(String newName) {
    individualID = newName;
  }

    public void setIndividualID(String newName) {
      individualID = newName;
    }


  /**
   * Returns the specified encounter, where the encounter numbers range from 0 to n-1, where n is the total number of encounters stored
   * for this MarkedIndividual.
   *
   * @return the encounter at position i in the stored Vector of encounters
   * @param  i  the specified encounter number, where i=0...(n-1)
   */
  public Encounter getEncounter(int i) {
    return (Encounter) encounters.get(i);
  }

  public Encounter getLogEncounter(int i) {
    return (Encounter) unidentifiableEncounters.get(i);
  }

  /**
   * Returns the complete Vector of stored encounters for this MarkedIndividual.
   *
   * @return a Vector of encounters
   * @see java.util.Vector
   */
  public Vector getEncounters() {
    return encounters;
  }

    //you can choose the order of the EncounterDateComparator
    public Encounter[] getDateSortedEncounters(boolean reverse) {
    Vector final_encs = new Vector();
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      final_encs.add(temp);
    }

    int finalNum = final_encs.size();
    Encounter[] encs2 = new Encounter[finalNum];
    for (int q = 0; q < finalNum; q++) {
      encs2[q] = (Encounter) final_encs.get(q);
    }
    EncounterDateComparator dc = new EncounterDateComparator(reverse);
    Arrays.sort(encs2, dc);
    return encs2;
  }
  
  //sorted with the most recent first
  public Encounter[] getDateSortedEncounters() {return getDateSortedEncounters(false);}
  
  
  //preserved for legacy purposes
 /** public Encounter[] getDateSortedEncounters(boolean includeLogEncounters) {
    return getDateSortedEncounters();
  }
  */

  public Vector getUnidentifiableEncounters() {
    if (unidentifiableEncounters == null) {
      unidentifiableEncounters = new Vector();
    }
    return unidentifiableEncounters;
  }

  /**
   * Returns any additional, general comments recorded for this MarkedIndividual as a whole.
   *
   * @return a String of comments
   */
  public String getComments() {
    if (comments != null) {

      return comments;
    } else {
      return "None";
    }
  }

  /**
   * Adds any general comments recorded for this MarkedIndividual as a whole.
   *
   * @return a String of comments
   */
  public void addComments(String newComments) {
    if ((comments != null) && (!(comments.equals("None")))) {
      comments += newComments;
    } else {
      comments = newComments;
    }
  }

  /**
   * Returns the complete Vector of stored satellite tag data files for this MarkedIndividual.
   *
   * @return a Vector of Files
   * @see java.util.Vector
   */
  public Vector getDataFiles() {
    return dataFiles;
  }

  /**
   * Returns the sex of this MarkedIndividual.
   *
   * @return a String
   */
  public String getSex() {
    return sex;
  }

  /**
   * Sets the sex of this MarkedIndividual.
   */
  public void setSex(String newSex) {
    sex = newSex;
  }


  public double getLastEstimatedSize() {
    double lastSize = 0;
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > lastSize)) {
        lastSize = temp.getSize();
      }
    }
    return lastSize;
  }

  public boolean wasSightedInLocationCode(String locationCode) {
   
        for (int c = 0; c < encounters.size(); c++) {
          try{
            Encounter temp = (Encounter) encounters.get(c);
          
            if ((temp.getLocationID()!=null)&&(!temp.getLocationID().trim().equals(""))&&(temp.getLocationID().trim().equals(locationCode))) {
              return true;
            }
          }
          catch(NullPointerException npe){return false;} 
        }
         
        return false;
    }
    


  public ArrayList<String> participatesInTheseVerbatimEventDates() {
    ArrayList<String> vbed = new ArrayList<String>();
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getVerbatimEventDate() != null) && (!vbed.contains(temp.getVerbatimEventDate()))) {
        vbed.add(temp.getVerbatimEventDate());
      }
    }
    return vbed;
  }

    public ArrayList<String> participatesInTheseLocationIDs() {
      ArrayList<String> vbed = new ArrayList<String>();
      for (int c = 0; c < encounters.size(); c++) {
        Encounter temp = (Encounter) encounters.get(c);
        if ((temp.getLocationID() != null) && (!vbed.contains(temp.getLocationID()))) {
          vbed.add(temp.getLocationID());
        }
      }
      return vbed;
  }

  public boolean wasSightedInVerbatimEventDate(String ved) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getVerbatimEventDate() != null) && (temp.getVerbatimEventDate().equals(ved))) {
        return true;
      }
    }
    return false;
  }

  public boolean wasSightedByUser(String user) {
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getSubmitterID() != null) && (temp.getSubmitterID().equals(user))) {
        return true;
      }
    }
    return false;
  }

  public int getMaxNumYearsBetweenSightings(){
    return maxYearsBetweenResightings;
  }

  public int getEarliestSightingYear() {
    int lowestYear = 5000;
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() < lowestYear)&&(temp.getYear()>0)){ 
        lowestYear = temp.getYear();
      }
    }
    return lowestYear;
  }
  
  public long getEarliestSightingTime() {
    long lowestTime = GregorianCalendar.getInstance().getTimeInMillis();
    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getDateInMilliseconds() < lowestTime)&&(temp.getYear()>0)) lowestTime = temp.getDateInMilliseconds();
    }
    return lowestTime;
  }

  public String getSeriesCode() {
    return seriesCode;
  }

  public Vector getInterestedResearchers() {
    return interestedResearchers;
  }

  public void addInterestedResearcher(String email) {
    if(interestedResearchers==null){interestedResearchers=new Vector();}
      interestedResearchers.add(email);
    
  }

  public void removeInterestedResearcher(String email) {
    if(interestedResearchers!=null){
      for (int i = 0; i < interestedResearchers.size(); i++) {
        String rName = (String) interestedResearchers.get(i);
        if (rName.equals(email)) {
          interestedResearchers.remove(i);
        }
      }
    }
  }

  public void setSeriesCode(String newCode) {
    seriesCode = newCode;
  }

  /**
   * Adds a satellite tag data file for this MarkedIndividual.
   *
   * @param  dataFile  the satellite tag data file to be added
   */
  public void addDataFile(String dataFile) {
    if(dataFiles==null){dataFiles = new Vector();}
    dataFiles.add(dataFile);
  }

  /**
   * Removes a satellite tag data file for this MarkedIndividual.
   *
   * @param  dataFile  The satellite data file, as a String, to be removed.
   */
  public void removeDataFile(String dataFile) {
    if(dataFiles!=null)
    {
      dataFiles.remove(dataFile);
    }
  }

  public int getNumberTrainableEncounters() {
    int count = 0;
    for (int iter = 0; iter < encounters.size(); iter++) {
      Encounter enc = (Encounter) encounters.get(iter);
      if (enc.getSpots().size() > 0) {
        count++;
      }
    }
    return count;
  }


  public int getNumberRightTrainableEncounters() {
    int count = 0;
    for (int iter = 0; iter < encounters.size(); iter++) {
      Encounter enc = (Encounter) encounters.get(iter);
      if (enc.getRightSpots().size() > 0) {
        count++;
      }
    }
    return count;
  }

  public Vector getTrainableEncounters() {
    int count = 0;
    Vector results = new Vector();
    for (int iter = 0; iter < encounters.size(); iter++) {
      Encounter enc = (Encounter) encounters.get(iter);
      if (enc.getSpots().size() > 0) {
        results.add(enc);
      }
    }
    return results;
  }

  public Vector getRightTrainableEncounters() {
    int count = 0;
    Vector results = new Vector();
    for (int iter = 0; iter < encounters.size(); iter++) {
      Encounter enc = (Encounter) encounters.get(iter);
      if (enc.getRightSpots().size() > 0) {
        results.add(enc);
      }
    }
    return results;
  }

  /*public int getFirstTrainingEncounter() {
     for(int iter=0;iter<encounters.size(); iter++) {
       encounter enc=(encounter)encounters.get(iter);
       if (enc.getSpots()!=null) {return iter;}
       }
     return 0;
   }*/

  /*public int getSecondTrainingEncounter() {
     for(int iter=(getFirstTrainingEncounter()+1);iter<encounters.size(); iter++) {
       encounter enc=(encounter)encounters.get(iter);
       if (enc.getSpots()!=null) {return iter;}
       }
     return 0;
   }*/


  //months 1-12, days, 1-31
  /**
   * 
   * 
   * @deprecated
   */
  public double avgLengthInPeriod(int m_startYear, int m_startMonth, int m_endYear, int m_endMonth) {

    double avgLength = 0;
    int numMeasurements = 0;

    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int startYear = m_startYear;
    int startMonth = m_startMonth;

    //test that start and end dates are not reversed
    if (endYear < startYear) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    } else if ((endYear == startYear) && (endMonth < startMonth)) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    }

    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if ((temp.getYear() > startYear) && (temp.getYear() < endYear)) {
        if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0)) {
          avgLength += temp.getSize();
          numMeasurements++;
        }
      } else if ((temp.getYear() == startYear) && (temp.getYear() < endYear) && (temp.getMonth() >= startMonth)) {
        if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0)) {
          avgLength += temp.getSize();
          numMeasurements++;
        }
      } else if ((temp.getYear() > startYear) && (temp.getYear() == endYear) && (temp.getMonth() <= endMonth)) {
        if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0)) {
          avgLength += temp.getSize();
          numMeasurements++;
        }
      } else if ((temp.getYear() >= startYear) && (temp.getYear() <= endYear) && (temp.getMonth() >= startMonth) && (temp.getMonth() <= endMonth)) {
        if ((temp.getSizeAsDouble()!=null)&&(temp.getSize() > 0)) {
          avgLength += temp.getSize();
          numMeasurements++;
        }
      }


    }
    if (numMeasurements > 0) {
      return (avgLength / numMeasurements);
    } else {
      return 0.0;
    }
  }
  
  public Double getAverageMeasurementInPeriod(int m_startYear, int m_startMonth, int m_endYear, int m_endMonth, String measurementType) {

    double avgMeasurement = 0;
    int numMeasurements = 0;
    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int startYear = m_startYear;
    int startMonth = m_startMonth;

    //test that start and end dates are not reversed
    if (endYear < startYear) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    } else if ((endYear == startYear) && (endMonth < startMonth)) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    }

    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);
      if(temp.hasMeasurement(measurementType)){
        List<Measurement> measures=temp.getMeasurements();
        if ((temp.getYear() > startYear) && (temp.getYear() < endYear)) {
          if (temp.getMeasurement(measurementType)!=null) {
            avgMeasurement += temp.getMeasurement(measurementType).getValue();
            numMeasurements++;
          }
        } 
        else if ((temp.getYear() == startYear) && (temp.getYear() < endYear) && (temp.getMonth() >= startMonth)) {
          if (temp.getMeasurement(measurementType)!=null){
            avgMeasurement += temp.getMeasurement(measurementType).getValue();
            numMeasurements++;
          }
        } 
        else if ((temp.getYear() > startYear) && (temp.getYear() == endYear) && (temp.getMonth() <= endMonth)) {
          if (temp.getMeasurement(measurementType)!=null) {
            avgMeasurement += temp.getMeasurement(measurementType).getValue();
            numMeasurements++;
          }
        } 
        else if ((temp.getYear() >= startYear) && (temp.getYear() <= endYear) && (temp.getMonth() >= startMonth) && (temp.getMonth() <= endMonth)) {
          if (temp.getMeasurement(measurementType)!=null) {
            avgMeasurement += temp.getMeasurement(measurementType).getValue();
            numMeasurements++;
          }
        } 
      }
    }
    if (numMeasurements > 0) {
      return (new Double(avgMeasurement / numMeasurements));
    } 
    else {
      return null;
    }
  }
  
  public Double getAverageBiologicalMeasurementInPeriod(int m_startYear, int m_startMonth, int m_endYear, int m_endMonth, String measurementType) {

    double avgMeasurement = 0;
    int numMeasurements = 0;
    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int startYear = m_startYear;
    int startMonth = m_startMonth;

    //test that start and end dates are not reversed
    if (endYear < startYear) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    } else if ((endYear == startYear) && (endMonth < startMonth)) {
      endYear = m_startYear;
      endMonth = m_startMonth;
      startYear = m_endYear;
      startMonth = m_endMonth;
    }

    for (int c = 0; c < encounters.size(); c++) {
      Encounter enc = (Encounter) encounters.get(c);
      if((enc.getTissueSamples()!=null)&&(enc.getTissueSamples().size()>0)){
        List<TissueSample> samples=enc.getTissueSamples();
        int numTissueSamples=samples.size();
        for(int h=0;h<numTissueSamples;h++){
          TissueSample temp=samples.get(h);

          if(temp.hasMeasurement(measurementType)){
            List<BiologicalMeasurement> measures=temp.getBiologicalMeasurements();
            if ((enc.getYear() > startYear) && (enc.getYear() < endYear)) {
              if (temp.getBiologicalMeasurement(measurementType)!=null) {
                avgMeasurement += temp.getBiologicalMeasurement(measurementType).getValue();
                numMeasurements++;
              }
            } 
            else if ((enc.getYear() == startYear) && (enc.getYear() < endYear) && (enc.getMonth() >= startMonth)) {
              if (temp.getBiologicalMeasurement(measurementType)!=null){
                avgMeasurement += temp.getBiologicalMeasurement(measurementType).getValue();
                numMeasurements++;
              }
            } 
            else if ((enc.getYear() > startYear) && (enc.getYear() == endYear) && (enc.getMonth() <= endMonth)) {
              if (temp.getBiologicalMeasurement(measurementType)!=null) {
                avgMeasurement += temp.getBiologicalMeasurement(measurementType).getValue();
                numMeasurements++;
              }
            } 
            else if ((enc.getYear() >= startYear) && (enc.getYear() <= endYear) && (enc.getMonth() >= startMonth) && (enc.getMonth() <= endMonth)) {
              if (temp.getBiologicalMeasurement(measurementType)!=null) {
                avgMeasurement += temp.getBiologicalMeasurement(measurementType).getValue();
                numMeasurements++;
              }
            } 
          }
        }
      }
    }
    if (numMeasurements > 0) {
      return (new Double(avgMeasurement / numMeasurements));
    } 
    else {
      return null;
    }
  }

  public String getDateTimeCreated() {
    if (dateTimeCreated != null) {
      return dateTimeCreated;
    }
    return "";
  }

  public void setDateTimeCreated(String time) {
    dateTimeCreated = time;
  }

  public void setAlternateID(String newID) {
    this.alternateid = newID;
  }

  public String getAlternateID() {
    if (alternateid == null) {
      return "None";
    }
    return alternateid;
  }

  /*
   * Returns a bracketed, comma-delimited string of all of the alternateIDs registered for this marked individual, including those only assigned at the Encounter level
   */
   public String getAllAlternateIDs(){
     ArrayList<String> allIDs = new ArrayList<String>();

      //add any alt IDs for the individual itself
      if(alternateid!=null){allIDs.add(alternateid);}

      //add an alt IDs for the individual's encounters
      int numEncs=encounters.size();
      for(int c=0;c<numEncs;c++) {
        Encounter temp=(Encounter)encounters.get(c);
        if((temp.getAlternateID()!=null)&&(!temp.getAlternateID().equals("None"))&&(!allIDs.contains(temp.getAlternateID()))) {allIDs.add(temp.getAlternateID());}
      }

      return allIDs.toString();
    }

  public String getDynamicProperties() {
    return dynamicProperties;
  }

  public void setDynamicProperty(String name, String value) {
    name = name.replaceAll(";", "_").trim().replaceAll("%20", " ");
    value = value.replaceAll(";", "_").trim();

    if (dynamicProperties == null) {
      dynamicProperties = name + "=" + value + ";";
    } else {

      //let's create a TreeMap of the properties
      TreeMap<String, String> tm = new TreeMap<String, String>();
      StringTokenizer st = new StringTokenizer(dynamicProperties, ";");
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        int equalPlace = token.indexOf("=");
        tm.put(token.substring(0, equalPlace), token.substring(equalPlace + 1));
      }
      if (tm.containsKey(name)) {
        tm.remove(name);
        tm.put(name, value);

        //now let's recreate the dynamicProperties String
        String newProps = tm.toString();
        int stringSize = newProps.length();
        dynamicProperties = newProps.substring(1, (stringSize - 1)).replaceAll(", ", ";") + ";";
      } else {
        dynamicProperties = dynamicProperties + name + "=" + value + ";";
      }
    }
  }

  public String getDynamicPropertyValue(String name){
    if(dynamicProperties!=null){
      name=name.replaceAll("%20", " ");
      //let's create a TreeMap of the properties
      TreeMap<String,String> tm=new TreeMap<String,String>();
      StringTokenizer st=new StringTokenizer(dynamicProperties, ";");
      while(st.hasMoreTokens()){
        String token = st.nextToken();
        int equalPlace=token.indexOf("=");
        try{
          tm.put(token.substring(0,equalPlace), token.substring(equalPlace+1));
        }
        catch(IndexOutOfBoundsException ioob){}
      }
      if(tm.containsKey(name)){return tm.get(name);}
    }
    return null;
  }

  public void removeDynamicProperty(String name) {
    name = name.replaceAll(";", "_").trim().replaceAll("%20", " ");
    if (dynamicProperties != null) {

      //let's create a TreeMap of the properties
      TreeMap<String, String> tm = new TreeMap<String, String>();
      StringTokenizer st = new StringTokenizer(dynamicProperties, ";");
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        int equalPlace = token.indexOf("=");
        tm.put(token.substring(0, (equalPlace)), token.substring(equalPlace + 1));
      }
      if (tm.containsKey(name)) {
        tm.remove(name);

        //now let's recreate the dynamicProperties String
        String newProps = tm.toString();
        int stringSize = newProps.length();
        dynamicProperties = newProps.substring(1, (stringSize - 1)).replaceAll(", ", ";") + ";";
      }
    }
  }

  public ArrayList<Keyword> getAllAppliedKeywordNames(Shepherd myShepherd) {
    ArrayList<Keyword> al = new ArrayList<Keyword>();
    int numEncounters = encounters.size();
    for (int i = 0; i < numEncounters; i++) {
      Encounter enc = (Encounter) encounters.get(i);
      Iterator it = myShepherd.getAllKeywords();
      while (it.hasNext()) {
        Keyword word = (Keyword) it.next();
        if (enc.hasKeyword(word) && (!al.contains(word))) {
          al.add(word);
        }
      }
    }
    return al;
  }
  
  public ArrayList<TissueSample> getAllTissueSamples() {
    ArrayList<TissueSample> al = new ArrayList<TissueSample>();
    int numEncounters = encounters.size();
    for (int i = 0; i < numEncounters; i++) {
      Encounter enc = (Encounter) encounters.get(i);
      if(enc.getTissueSamples()!=null){
        List<TissueSample> list = enc.getTissueSamples();
        if(list.size()>0){
          al.addAll(list);
        }
      }
    }
    return al;
  }
  
  public ArrayList<SinglePhotoVideo> getAllSinglePhotoVideo() {
    ArrayList<SinglePhotoVideo> al = new ArrayList<SinglePhotoVideo>();
    int numEncounters = encounters.size();
    for (int i = 0; i < numEncounters; i++) {
      Encounter enc = (Encounter) encounters.get(i);
      if(enc.getSinglePhotoVideo()!=null){
        List<SinglePhotoVideo> list = enc.getSinglePhotoVideo();
        if(list.size()>0){
          al.addAll(list);
        }
      }
    }
    return al;
  }

  public ArrayList<String> getAllValuesForDynamicProperty(String propertyName) {
    ArrayList<String> listPropertyValues = new ArrayList<String>();

    //first, check if the individual has the property applied
    if (getDynamicPropertyValue(propertyName) != null) {
      listPropertyValues.add(getDynamicPropertyValue(propertyName));
    }

    //next check the encounters
    int numEncounters = encounters.size();
    for (int i = 0; i < numEncounters; i++) {
      Encounter enc = (Encounter) encounters.get(i);
      if (enc.getDynamicPropertyValue(propertyName) != null) {
        listPropertyValues.add(enc.getDynamicPropertyValue(propertyName));
      }
    }
    return listPropertyValues;
  }

  /**
  Returns the patterning type evident on this MarkedIndividual instance.

  */
  public String getPatterningCode(){

    int numEncs=encounters.size();
    for(int i=0;i<numEncs;i++){
      Encounter enc=(Encounter)encounters.get(i);
      if(enc.getPatterningCode()!=null){return enc.getPatterningCode();}
    }
    return null;
  }

  /**
  Sets the patterning type evident on this MarkedIndividual instance.

  */
  public void setPatterningCode(String newCode){this.patterningCode=newCode;}

  public void resetMaxNumYearsBetweenSightings(){
    int maxYears=0;
    int lowestYear=3000;
    int highestYear=0;
    for(int c=0;c<encounters.size();c++) {
      Encounter temp=(Encounter)encounters.get(c);
      if((temp.getYear()<lowestYear)&&(temp.getYear()>0)) lowestYear=temp.getYear();
      if(temp.getYear()>highestYear) highestYear=temp.getYear();
      maxYears=highestYear-lowestYear;
      if(maxYears<0){maxYears=0;}
      }
    maxYearsBetweenResightings=maxYears;
    }

  public String sidesSightedInPeriod(int m_startYear, int m_startMonth, int m_startDay, int m_endYear, int m_endMonth, int m_endDay, String locCode) {
    int endYear = m_endYear;
    int endMonth = m_endMonth;
    int endDay = m_endDay;
    int startYear = m_startYear;
    int startMonth = m_startMonth;
    int startDay = m_startDay;

    GregorianCalendar gcMin=new GregorianCalendar(startYear, startMonth-1, startDay);
    GregorianCalendar gcMax=new GregorianCalendar(endYear, endMonth-1, endDay);

    boolean left=false;
    boolean right=false;
    boolean leftRightTogether=false;


    for (int c = 0; c < encounters.size(); c++) {
      Encounter temp = (Encounter) encounters.get(c);

      if (temp.getLocationCode().startsWith(locCode)) {

        if((temp.getDateInMilliseconds()>=gcMin.getTimeInMillis())&&(temp.getDateInMilliseconds()<=gcMax.getTimeInMillis())){
          if(temp.getNumRightSpots()>0){right=true;}
          if(temp.getNumSpots()>0){left=true;}
          if((temp.getNumRightSpots()>0)&&(temp.getNumSpots()>0)){leftRightTogether=true;}
        }
      }
    }
    if(leftRightTogether){return "3";}
    else if(left&&right){return "4";}
    else if(left){return "1";}
    else if(right){return "2";}
    else{
      return "0";
    }
  }
/**
Returns the first genus-species pair found in the Encounter objects for this MarkedIndividual.
@return a String if found or null if no genus-species pair is found
*/
public String getGenusSpecies(){
	    for (int c = 0; c < encounters.size(); c++) {
	      	Encounter temp = (Encounter) encounters.get(c);
			if((temp.getGenus()!=null)&&(temp.getSpecificEpithet()!=null)){return (temp.getGenus()+" "+temp.getSpecificEpithet());}

    	}
		return null;

}

/**
Returns the first haplotype found in the Encounter objects for this MarkedIndividual.
@return a String if found or null if no haplotype is found
*/
public String getHaplotype(){
      
    return localHaplotypeReflection;
    
}



public String getGeneticSex(){
  for (int c = 0; c < encounters.size(); c++) {
    Encounter temp = (Encounter) encounters.get(c);
    if(temp.getGeneticSex()!=null){return temp.getGeneticSex();}
  }
return null;

}

public boolean hasLocusAndAllele(String locus, Integer alleleValue){
  ArrayList<TissueSample> samples=getAllTissueSamples();
  int numSamples=samples.size();
  for(int i=0;i<numSamples;i++){
      TissueSample sample=samples.get(i);
      if(sample.getGeneticAnalyses()!=null){
        List<GeneticAnalysis> analyses=sample.getGeneticAnalyses();
        int numAnalyses=analyses.size();
        for(int e=0;e<numAnalyses;e++){
          GeneticAnalysis ga=analyses.get(e);
          if(ga.getAnalysisType().equals("MicrosatelliteMarkers")){
            MicrosatelliteMarkersAnalysis msa=(MicrosatelliteMarkersAnalysis)ga;
            if(msa.getLocus(locus)!=null){
               Locus l=msa.getLocus(locus);
               if(l.hasAllele(alleleValue)){return true;}
            }
          }
        }
      }
  }
  return false;
}

public ArrayList<Integer> getAlleleValuesForLocus(String locus){
  ArrayList<Integer> matchingValues=new ArrayList<Integer>();
  ArrayList<TissueSample> samples=getAllTissueSamples();
  int numSamples=samples.size();
  for(int i=0;i<numSamples;i++){
      TissueSample sample=samples.get(i);
      if(sample.getGeneticAnalyses()!=null){
        List<GeneticAnalysis> analyses=sample.getGeneticAnalyses();
        int numAnalyses=analyses.size();
        for(int e=0;e<numAnalyses;e++){
          GeneticAnalysis ga=analyses.get(e);
          if(ga.getAnalysisType().equals("MicrosatelliteMarkers")){
            MicrosatelliteMarkersAnalysis msa=(MicrosatelliteMarkersAnalysis)ga;
            if(msa.getLocus(locus)!=null){
               Locus l=msa.getLocus(locus);
               if((l.getAllele0()!=null)){matchingValues.add(l.getAllele0());}
               if((l.getAllele1()!=null)){matchingValues.add(l.getAllele1());}
               if((l.getAllele2()!=null)){matchingValues.add(l.getAllele2());}
               if((l.getAllele3()!=null)){matchingValues.add(l.getAllele3());}
            }
          }
        }
      }
  }
  return matchingValues;
}

public boolean hasLocus(String locus){
  ArrayList<TissueSample> samples=getAllTissueSamples();
  int numSamples=samples.size();
  for(int i=0;i<numSamples;i++){
      TissueSample sample=samples.get(i);
      if(sample.getGeneticAnalyses()!=null){
        List<GeneticAnalysis> analyses=sample.getGeneticAnalyses();
        int numAnalyses=analyses.size();
        for(int e=0;e<numAnalyses;e++){
          GeneticAnalysis ga=analyses.get(e);
          if(ga.getAnalysisType().equals("MicrosatelliteMarkers")){
            MicrosatelliteMarkersAnalysis msa=(MicrosatelliteMarkersAnalysis)ga;
            if(msa.getLocus(locus)!=null){
               return true;
            }
          }
        }
      }
  }
  return false;
}





public boolean hasMsMarkers(){
  ArrayList<TissueSample> samples=getAllTissueSamples();
  int numSamples=samples.size();
  for(int i=0;i<numSamples;i++){
      TissueSample sample=samples.get(i);
      if(sample.getGeneticAnalyses()!=null){
        List<GeneticAnalysis> analyses=sample.getGeneticAnalyses();
        int numAnalyses=analyses.size();
        for(int e=0;e<numAnalyses;e++){
          GeneticAnalysis ga=analyses.get(e);
          if(ga.getAnalysisType().equals("MicrosatelliteMarkers")){
            return true;
          }
        }
      }
  }
  return false;
}

public boolean hasGeneticSex(){
  ArrayList<TissueSample> samples=getAllTissueSamples();
  int numSamples=samples.size();
  for(int i=0;i<numSamples;i++){
      TissueSample sample=samples.get(i);
      if(sample.getGeneticAnalyses()!=null){
        List<GeneticAnalysis> analyses=sample.getGeneticAnalyses();
        int numAnalyses=analyses.size();
        for(int e=0;e<numAnalyses;e++){
          GeneticAnalysis ga=analyses.get(e);
          if(ga.getAnalysisType().equals("SexAnalysis")){
            return true;
          }
        }
      }
  }
  return false;
}


/**
*Obtains the email addresses of all submitters, photographs, and others to notify.
*@return ArrayList of all emails to inform
*/
public ArrayList getAllEmailsToUpdate(){
	ArrayList notifyUs=new ArrayList();

	int numEncounters=encounters.size();
	int numUnidetifiableEncounters=unidentifiableEncounters.size();

	//process encounters
	for(int i=0;i<numEncounters;i++){
		Encounter enc=(Encounter)encounters.get(i);
		if((enc.getSubmitterEmail()!=null)&&(!enc.getSubmitterEmail().trim().equals(""))){
			String submitter = enc.getSubmitterEmail();
			if (submitter.indexOf(",") != -1) {
			   StringTokenizer str = new StringTokenizer(submitter, ",");
			   while (str.hasMoreTokens()) {
        	         String token = str.nextToken().trim();
					 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
	   			}
			}
			else{if(!notifyUs.contains(submitter)){notifyUs.add(submitter);}}
		}
		if((enc.getPhotographerEmail()!=null)&&(!enc.getPhotographerEmail().trim().equals(""))){
					String photog = enc.getPhotographerEmail();
					if (photog.indexOf(",") != -1) {
					   StringTokenizer str = new StringTokenizer(photog, ",");
					   while (str.hasMoreTokens()) {
		        	         String token = str.nextToken().trim();
							 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
			   			}
					}
					else{if(!notifyUs.contains(photog)){notifyUs.add(photog);}}
		}
		if((enc.getInformOthers()!=null)&&(!enc.getInformOthers().trim().equals(""))){
							String photog = enc.getInformOthers();
							if (photog.indexOf(",") != -1) {
							   StringTokenizer str = new StringTokenizer(photog, ",");
							   while (str.hasMoreTokens()) {
				        	         String token = str.nextToken().trim();
									 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
					   			}
							}
							else{if(!notifyUs.contains(photog)){notifyUs.add(photog);}}
		}

	}

		//process log encounters
		for(int i=0;i<numUnidentifiableEncounters;i++){
			Encounter enc=(Encounter)unidentifiableEncounters.get(i);
			if((enc.getSubmitterEmail()!=null)&&(!enc.getSubmitterEmail().trim().equals(""))){
				String submitter = enc.getSubmitterEmail();
				if (submitter.indexOf(",") != -1) {
				   StringTokenizer str = new StringTokenizer(submitter, ",");
				   while (str.hasMoreTokens()) {
	        	         String token = str.nextToken().trim();
						 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
		   			}
				}
				else{if(!notifyUs.contains(submitter)){notifyUs.add(submitter);}}
			}
			if((enc.getPhotographerEmail()!=null)&&(!enc.getPhotographerEmail().trim().equals(""))){
						String photog = enc.getPhotographerEmail();
						if (photog.indexOf(",") != -1) {
						   StringTokenizer str = new StringTokenizer(photog, ",");
						   while (str.hasMoreTokens()) {
			        	         String token = str.nextToken().trim();
								 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
				   			}
						}
						else{if(!notifyUs.contains(photog)){notifyUs.add(photog);}}
			}
			if((enc.getInformOthers()!=null)&&(!enc.getInformOthers().trim().equals(""))){
								String photog = enc.getInformOthers();
								if (photog.indexOf(",") != -1) {
								   StringTokenizer str = new StringTokenizer(photog, ",");
								   while (str.hasMoreTokens()) {
					        	         String token = str.nextToken().trim();
										 if((!token.equals(""))&&(!notifyUs.contains(token))){notifyUs.add(token);}
						   			}
								}
								else{if(!notifyUs.contains(photog)){notifyUs.add(photog);}}
			}

	}

	return notifyUs;

}

public void removeLogEncounter(Encounter enc){if(unidentifiableEncounters.contains(enc)){unidentifiableEncounters.remove(enc);}}

public float distFrom(float lat1, float lng1, float lat2, float lng2) {
  double earthRadius = 3958.75;
  double dLat = Math.toRadians(lat2-lat1);
  double dLng = Math.toRadians(lng2-lng1);
  double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
             Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
             Math.sin(dLng/2) * Math.sin(dLng/2);
  double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  double dist = earthRadius * c;

  int meterConversion = 1609;

  return new Float(dist * meterConversion).floatValue();
}

public Float getMaxDistanceBetweenTwoSightings(){
  int numEncs=encounters.size();
  Float maxDistance=new Float(0);
  if(numEncs>1){
  for(int y=0;y<numEncs;y++){
    Encounter thisEnc=(Encounter)encounters.get(y);
    if((thisEnc.getLatitudeAsDouble()!=null)&&(thisEnc.getLongitudeAsDouble()!=null)){
    for(int z=(y+1);z<numEncs;z++){
      Encounter nextEnc=(Encounter)encounters.get(z);
      if((nextEnc.getLatitudeAsDouble()!=null)&&(nextEnc.getLongitudeAsDouble()!=null)){
        try{
          Float tempMaxDistance=distFrom(new Float(thisEnc.getLatitudeAsDouble()), new Float(thisEnc.getLongitudeAsDouble()), new Float(nextEnc.getLatitudeAsDouble()), new Float(nextEnc.getLongitudeAsDouble()));
          if(tempMaxDistance>maxDistance){maxDistance=tempMaxDistance;}
        }
        catch(Exception e){e.printStackTrace();System.out.println("Hit an NPE when calculating distance between: "+thisEnc.getCatalogNumber()+" and "+nextEnc.getCatalogNumber());}
      }
    }
  }
  }
  }
  return maxDistance;
}

public long getMaxTimeBetweenTwoSightings(){
  int numEncs=encounters.size();
  long maxTime=0;
  if(numEncs>1){
  for(int y=0;y<numEncs;y++){
    Encounter thisEnc=(Encounter)encounters.get(y);
    for(int z=(y+1);z<numEncs;z++){
      Encounter nextEnc=(Encounter)encounters.get(z);
      if(thisEnc.getDateInMilliseconds()>0){
        long tempMaxTime=Math.abs(thisEnc.getDateInMilliseconds()-nextEnc.getDateInMilliseconds());
        if(tempMaxTime>maxTime){maxTime=tempMaxTime;}
      }
    }
  }
  }
  return maxTime;
}

public ArrayList<String> getAllAssignedUsers(){
  ArrayList<String> allIDs = new ArrayList<String>();

   //add an alt IDs for the individual's encounters
   int numEncs=encounters.size();
   for(int c=0;c<numEncs;c++) {
     Encounter temp=(Encounter)encounters.get(c);
     if((temp.getAssignedUsername()!=null)&&(!allIDs.contains(temp.getAssignedUsername()))) {allIDs.add(temp.getAssignedUsername());}
   }

   return allIDs;
 }

/**
 * DO NOT SET DIRECTLY!!
 * 
 * @param myDepth
 */
public void doNotSetLocalHaplotypeReflection(String myHaplo) {
  if(myHaplo!=null){localHaplotypeReflection = myHaplo;}
  else{localHaplotypeReflection = null;}
}

public long getTimeOfBirth(){return timeOfBirth;}
public long getTimeofDeath(){return timeOfDeath;}

public void setTimeOfBirth(long newTime){timeOfBirth=newTime;}
public void setTimeOfDeath(long newTime){timeOfDeath=newTime;}

public ArrayList<Relationship> getAllRelationships(Shepherd myShepherd){
  return myShepherd.getAllRelationshipsForMarkedIndividual(individualID);
}

public String getFomattedMSMarkersString(String[] loci){
  StringBuffer sb=new StringBuffer();
  int numLoci=loci.length;
  for(int i=0;i<numLoci;i++){
    ArrayList<Integer> alleles=getAlleleValuesForLocus(loci[i]);
    if((alleles.size()>0)&&(alleles.get(0)!=null)){sb.append(alleles.get(0)+" ");}
    else{sb.append("--- ");}
    if((alleles.size()>=2)&&(alleles.get(1)!=null)){sb.append(alleles.get(1)+" ");}
    else{sb.append("--- ");}
  }
  return sb.toString();
}

public Float getMinDistanceBetweenTwoMarkedIndividuals(MarkedIndividual otherIndy){
  
  DecimalFormat df = new DecimalFormat("#.#");
  Float minDistance=new Float(1000000);
  if((encounters!=null)&&(encounters.size()>0)&&(otherIndy.getEncounters()!=null)&&(otherIndy.getEncounters().size()>0)){
  int numEncs=encounters.size();
  int numOtherEncs=otherIndy.getEncounters().size();
  
  if(numEncs>0){
  for(int y=0;y<numEncs;y++){
    Encounter thisEnc=(Encounter)encounters.get(y);
    if((thisEnc.getLatitudeAsDouble()!=null)&&(thisEnc.getLongitudeAsDouble()!=null)){
    for(int z=0;z<numOtherEncs;z++){
      Encounter nextEnc=otherIndy.getEncounter(z);
      if((nextEnc.getLatitudeAsDouble()!=null)&&(nextEnc.getLongitudeAsDouble()!=null)){
        try{
          Float tempMinDistance=distFrom(new Float(thisEnc.getLatitudeAsDouble()), new Float(thisEnc.getLongitudeAsDouble()), new Float(nextEnc.getLatitudeAsDouble()), new Float(nextEnc.getLongitudeAsDouble()));
          if(tempMinDistance<minDistance){minDistance=tempMinDistance;}
        }
        catch(Exception e){e.printStackTrace();System.out.println("Hit an NPE when calculating distance between: "+thisEnc.getCatalogNumber()+" and "+nextEnc.getCatalogNumber());}
      }
    }
  }
  }
  }
  }
  if(minDistance>999999)minDistance=new Float(-1);
  return minDistance;
}

}