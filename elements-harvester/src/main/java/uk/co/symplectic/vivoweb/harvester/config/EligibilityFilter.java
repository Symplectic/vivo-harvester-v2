/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.config;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;

import java.util.Collections;
import java.util.Set;

/**
 * Created by ajpc2_000 on 22/06/2017.
 */
public class EligibilityFilter {
    private final boolean academicsOnly;
    private final boolean currentStaffOnly;

    protected EligibilityFilter(boolean academicsOnly, boolean currentStaffOnly){
        this.academicsOnly = academicsOnly;
        this.currentStaffOnly = currentStaffOnly;
    }

    public final boolean isUserEligible(ElementsUserInfo userToTest){
        if(this.academicsOnly && !userToTest.getIsAcademic()) return false;
        if(this.currentStaffOnly && !userToTest.getIsCurrentStaff()) return false;
        return innerIsUserEligible(userToTest);
    }

    protected boolean innerIsUserEligible(ElementsUserInfo userToTest) {return true;}

    public static abstract class InclusionExclusionFilter extends EligibilityFilter {
        private final String schemeName;
        private final String inclusionValue;
        private final String exclusionValue;

        public String getName(){return schemeName; }

        protected InclusionExclusionFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly){
            super(academicsOnly, currentStaffOnly);
            if(StringUtils.trimToNull(schemeName) == null) throw new NullArgumentException("name");
            this.schemeName = schemeName;

            this.inclusionValue = StringUtils.trimToNull(inclusionValue);
            this.exclusionValue = StringUtils.trimToNull(exclusionValue);
            if(this.inclusionValue == null && this.exclusionValue == null) throw new IllegalArgumentException("One of inclusionValue and exclusionValue must not be null when instantiating an InclusionExclusionFilter ");
        }

        @Override
        protected boolean innerIsUserEligible(ElementsUserInfo userToTest) {
            Set<String> valuesToTest = getValuesToTest(userToTest);
            //if an exclusion is configured and it trips then we just are not interested - exclusions win over inclusions..
            if(exclusionValue != null && valuesToTest.contains(exclusionValue)) return false;
            //otherwise if not yet excluded and an inclusion is configured that does not trip, then we just are not interested..
            if(inclusionValue != null && !valuesToTest.contains(inclusionValue)) return false;
            return true;
        }

        protected abstract Set<String> getValuesToTest(ElementsUserInfo userToTest);
    }

    public static class LabelSchemeFilter extends InclusionExclusionFilter{

        public LabelSchemeFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly){
            super(schemeName, inclusionValue, exclusionValue, academicsOnly, currentStaffOnly);
        }

        @Override
        protected Set<String> getValuesToTest(ElementsUserInfo userToTest){ return userToTest.getLabelSchemeValues(); }
    }

    public static class GenericFieldFilter extends InclusionExclusionFilter{

        public GenericFieldFilter(String schemeName, String inclusionValue, String exclusionValue, boolean academicsOnly, boolean currentStaffOnly){
            super(schemeName, inclusionValue, exclusionValue, academicsOnly, currentStaffOnly);
        }

        @Override
        protected Set<String> getValuesToTest(ElementsUserInfo userToTest){ return Collections.singleton(userToTest.getGenericFieldValue()); }
    }


}
