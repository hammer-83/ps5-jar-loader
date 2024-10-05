package org.ps5jb.client.payloads.umtx;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.kernel.KernelPointer;

class KernelAddressClassifier {
    Map counts = new TreeMap();

    static KernelAddressClassifier fromBuffer(MemoryBuffer buffer) {
        KernelAddressClassifier result = new KernelAddressClassifier();
        for (int i = 0; i < buffer.getSize(); i += 8) {
            KernelPointer kptr = new KernelPointer(buffer.read64(i));
            try {
                KernelPointer.validRange(kptr);

                Long val = new Long(kptr.addr());
                Integer curCount = (Integer) result.counts.get(val);
                if (curCount == null) {
                    curCount = new Integer(0);
                } else {
                    curCount = new Integer(curCount.intValue() + 1);
                }
                result.counts.put(val, curCount);
            } catch (IllegalAccessError e) {
                // Ignore
            }
        }
        return result;
    }

    void dump() {
        Iterator iter = counts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry count = (Map.Entry) iter.next();
            DebugStatus.info(Long.toHexString((Long) count.getKey()) + ": " + count.getValue());
        }
    }

    Long getMostOccuredHeapAddress(int threshold) {
        Long result = null;
        int maxCount = 0;
        Iterator iter = counts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry count = (Map.Entry) iter.next();
            int countVal = ((Integer) count.getValue()).intValue();
            if (countVal > maxCount && countVal >= threshold) {
                result = (Long) count.getKey();
            }
        }
        return result;
    }
}
