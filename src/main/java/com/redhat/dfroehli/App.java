package com.redhat.dfroehli;

import java.util.Random;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.util.CloseableIteratorCollection;

class Rtu {
    String rtuId;

    String signalSource = "RTU000001";

    long linuxTimestamp;

    long qualityCode;

    double value;
}

public class App {

    /*
     * # rtu-id; signal source; linux timestamp; quality code; value
     * RES_LINE$I_FROM_KA$013035;RTU000001;1546300800001;192;0.11795469432801944
     */

    private RemoteCache<String, String> cache;

    public void connectToCache() {
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        cb.marshaller(new org.infinispan.commons.marshall.ProtoStreamMarshaller()).statistics().enable()
                .jmxDomain("org.example").addServer().host("127.0.0.1").port(11222);

        RemoteCacheManager rmc = new RemoteCacheManager(cb.build(), true);

        cache = rmc.getCache("FTS");
        if (cache == null) {
            throw new RuntimeException("Cache >FTS< not found");
        }
    }

    public void createEntries() {
        Rtu val = new Rtu();
        Random rnd = new Random();

        long numValues = 100000;
        long ts_start = System.currentTimeMillis();
        val.linuxTimestamp = System.currentTimeMillis();
        val.qualityCode = rnd.nextInt(256);
        val.value = rnd.nextDouble();
        // ToDO: use proto buf marshalling:
        String csv = val.rtuId + ";" + val.linuxTimestamp + ";" + val.qualityCode + ";" + val.value;

        for (int i = 0; i < numValues; i++) {
            // val.rtuId = "RES_LINE$I_FROM_KA$"+String.format("%06d" , i);
            val.rtuId = "RES_LINE$I_FROM_KA$" + i;

            cache.put(val.rtuId, csv);

            if (i % 10000 == 0) {
                System.out.print('.');
            }
        }
        long ts_stop = System.currentTimeMillis();
        long dur = ts_stop - ts_start;
        System.out.println("Creation of " + numValues + " took " + dur + " msec, that is "
                + (int) (numValues / (dur / 1000.0)) + " entries per second");
    }

    public void dumpEntries() {

        CloseableIteratorCollection<String> it = cache.values();
        StringBuffer sb = new StringBuffer(1000000);
        long numValues = 0;
        long ts_start = System.currentTimeMillis();

        for (String s : it) {
//            sb.append(s);
            numValues++;    
            if (numValues % 10000 == 0) {
                System.out.print('.');
            }
        }

        long ts_stop = System.currentTimeMillis();
        long dur = ts_stop - ts_start;
        System.out.println("Dumping of " + numValues + " took " + dur + " msec, that is "
                + (int) (numValues / (dur / 1000.0)) + " entries per second");
    }


    public static void main(String[] args) throws Exception {

        App a = new App();
        a.connectToCache();
        a.createEntries();
        a.dumpEntries();
    }
}

/* Example Cache Configuration POST against http://localhost:11222/rest/v2/FTS
<infinispan>
    <cache-container>
        <distributed-cache mode="SYNC" name="dummy" owners="2">
            <memory>
                <object size="1000000" strategy="REMOVE"/>
            </memory>
            <expiration lifespan="-1" max-idle="-1" interval="0" />
            <partition-handling when-split="ALLOW_READS"/>
            <persistence>
                <file-store shared="false" fetch-state="true" preload="true" max-entries="1000000">
                    <write-behind modification-queue-size="1000" fail-silently="false"/>
                </file-store>
            </persistence>
        </distributed-cache>
    </cache-container>
</infinispan>

*/

