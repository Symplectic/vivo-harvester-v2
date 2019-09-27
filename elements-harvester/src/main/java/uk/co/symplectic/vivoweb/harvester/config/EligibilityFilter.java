/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.config;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;

import java.util.Collections;
import java.util.Set;

/**
 * Class representing a filter to exclude some users from being sent to vivo.
 * There may be additional reasons that a user is not sent (e.g. group membership), this is just one layer
 * The simplest type operates on whether the user is academic/current/public outcome depending on configuration
 */
public class EligibilityFilter {
    private final boolean academicsOnly;
    private final boolean currentStaffOnly;
    private final boolean publicOnly;

    public boolean filtersOutNonPublicStaff(){ return publicOnly; }

    EligibilityFilter(boolean academicsOnly, boolean currentStaffOnly, boolean publicOnly){
        this.academicsOnly = academicsOnly;
        this.currentStaffOnly = currentStaffOnly;
        this.publicOnly = publicOnly;
    }

    public final boolean isUserEligible(ElementsUserInfo userToTest){
        if(this.academicsOnly && !userToTest.getIsAcademic()) return false;
        if(this.currentStaffOnly && !userToTest.getIsCurrentStaff()) return false;
        //noinspection SimplifiableIfStatement
        if(this.publicOnly && !userToTest.getIsPublic()) return false;
        return innerIsUserEligible(userToTest);
    }

    boolean innerIsUserEligible(ElementsUserInfo userToTest) {return true;}

    /**
     * Abstract class based on including or excluding a user because they have a particular value "somewhere".
     */
    public static abstract class InclusionExclusionFilter extends EligibilityFilter {
        private final String schemeName;
        private final String inclusionValue;
        private final String exclusionValue;

        public String getName(){return schemeName; }

        InclusionExclusionFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly, boolean publicOnly){
            super(academicsOnly, currentStaffOnly, publicOnly);
            if(StringUtils.trimToNull(schemeName) == null) throw new NullArgumentException("name");
            this.schemeName = schemeName;

            this.inclusionValue = StringUtils.trimToNull(inclusionValue);
            this.exclusionValue = StringUtils.trimToNull(exclusionValue);
            if(this.inclusionValue == null && this.exclusionValue == null) throw new IllegalArgumentException("One of inclusionValue and exclusionValue must not be null when instantiating an InclusionExclusionFilter ");
        }

        @Override
        boolean innerIsUserEligible(ElementsUserInfo userToTest) {
            Set<String> valuesToTest = getValuesToTest(userToTest);
            //if an exclusion is configured and it trips then we just are not interested - exclusions win over inclusions..
            if(exclusionValue != null && valuesToTest.contains(exclusionValue)) return false;
            //otherwise if not yet excluded and an inclusion is configured that does not trip, then we just are not interested..
            //noinspection RedundantIfStatement
            if(inclusionValue != null && !valuesToTest.contains(inclusionValue)) return false;
            return true;
        }

        protected abstract Set<String> getValuesToTest(ElementsUserInfo userToTest);
    }

    /**
     * A filter that operates based on a specific label-scheme
     */
    public static class LabelSchemeFilter extends InclusionExclusionFilter{

        LabelSchemeFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly, boolean publicOnly){
            super(schemeName, inclusionValue, exclusionValue, academicsOnly, currentStaffOnly, publicOnly);
        }

        @Override
        protected Set<String> getValuesToTest(ElementsUserInfo userToTest){ return userToTest.getLabelSchemeValues(); }
    }

    /**
     * A filter that operates based on a specific generic field.
     */
    public static class GenericFieldFilter extends InclusionExclusionFilter{

        GenericFieldFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly, boolean publicOnly){
            super(schemeName, inclusionValue, exclusionValue, academicsOnly, currentStaffOnly, publicOnly);
        }

        @Override
        protected Set<String> getValuesToTest(ElementsUserInfo userToTest){ return Collections.singleton(userToTest.getGenericFieldValue()); }
    }


}
