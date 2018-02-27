package com.netflix.vms.transformer.hollowinput;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.AbstractHollowUniqueKeyIndex;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class AbsoluteSchedulePrimaryKeyIndex extends AbstractHollowUniqueKeyIndex<VMSHollowInputAPI, AbsoluteScheduleHollow> {

    public AbsoluteSchedulePrimaryKeyIndex(HollowConsumer consumer) {
        this(consumer, ((HollowObjectSchema)consumer.getStateEngine().getSchema("AbsoluteSchedule")).getPrimaryKey().getFieldPaths());
    }

    public AbsoluteSchedulePrimaryKeyIndex(HollowConsumer consumer, String... fieldPaths) {
        this(consumer, true, fieldPaths);
    }

    public AbsoluteSchedulePrimaryKeyIndex(HollowConsumer consumer, boolean isListenToDataRefreah, String... fieldPaths) {
        super(consumer, "AbsoluteSchedule", isListenToDataRefreah, fieldPaths);
    }

    public AbsoluteScheduleHollow findMatch(Object... keys) {
        int ordinal = idx.getMatchingOrdinal(keys);
        if(ordinal == -1)
            return null;
        return api.getAbsoluteScheduleHollow(ordinal);
    }

}