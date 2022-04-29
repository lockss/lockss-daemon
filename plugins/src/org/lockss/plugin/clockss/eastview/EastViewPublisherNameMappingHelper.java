/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.eastview;

import org.lockss.util.Logger;

import java.util.*;

public class EastViewPublisherNameMappingHelper {

    private static final Logger log = Logger.getLogger(org.lockss.plugin.clockss.eastview.EastViewPublisherNameMappingHelper.class);

    protected static final Map<String, String> canonical;

    /*
    Title,Publication Acronym,Database Acronym
    Pravda,PRDMF,DA-PRA
    Listok pravdy,PRDMFLP,DA-PRA
    Petrogradskaia pravda,PRDMFPE,DA-PRA
    Pravda. St. Petersburg  ,PRDMFPA,DA-PRA
    Pravda. Petrograd   ,PRDMFPR,DA-PRA
    Pravda (Petrogradskaia pravda),PRDMFPV,DA-PRA
    Pravda (Sotsial-Demokrat),PRDMFSD,DA-PRA
    Pravda truda,PRDMFPT,DA-PRA
    Proletarii,PRDMFPO,DA-PRA
    Proletarskaia pravda,PRDMFPP,DA-PRA
    Put' pravdy,PRDMFPU,DA-PRA
    Rabochaia pravda,PRDMFRB,DA-PRA
    Rabochii,PRDMFRA,DA-PRA
    Rabochii I soldat,PRDMFRS,DA-PRA
    Rabochii put',PRDMFRP,DA-PRA
    Severnaia pravda,PRDMFSP,DA-PRA
    Trudovaia pravda,PRDMFTP,DA-PRA
    Za pravdu,PRDMFZP,DA-PRA
    Sovetskaia kul'tura,SKULMF,DA-SK
    Kul'tura ,KULMF,DA-SK
    Literatura i iskusstvo ,LIKULMF,DA-SK
    Rabochii i iskusstvo ,RKULMF,DA-SK
    Sovetskoe iskusstvo ,SOVKULMF,DA-SK
    Moscow News ,MDN,DA-MN
    Moscow Daily News,MOSN,DA-MN
    Novoe Russkoe Slovo ,DANRS,DA-NRS
    Russkoe Slovo,DARS,DA-NRS
    Pravda Ukrainy,PRUK,DA-PU
    Sovetskaia Ukraina,SOUK,DA-PU
    Slovo Kyrgystana ,SKYR,DA-SKYR
    Batratskaia pravda,BAPR,DA-SKYR
    Krest'ianskii put',KRPU,DA-SKYR
    Sovetskaia Kirgiziia,SOKI,DA-SKYR
    Avangard,DAAVAN,DA-BELP
    Bal'shavitski shliakh,DABOLS,DA-BELP
    Bal'shavitski stsiag,DABOLG,DA-BELP
    Chyrvonaia zara,DACHER,DA-BELP
    Chyrvony khlebarob,DAHLEB,DA-BELP
    Chyrvony stsiag,DACHEG,DA-BELP
    Gomel'skaia pra_da,DAGOML,DA-BELP
    Kalgasnaia trybuna,DAKALT,DA-BELP
    Kalgasnik Kapyl'shchyny,DAKALG,DA-BELP
    Kalgasny shliakh,DAKALH,DA-BELP
    Kamunar,DAKOM,DA-BELP
    Klich Radzimy,DAKLIK,DA-BELP
    Leninski shliakh,DALEN,DA-BELP
    My otomstim,DAMOM,DA-BELP
    Novaia veska,DANOVA,DA-BELP
    Plamia,DAPLAM,DA-BELP
    Po stalinskomu puti,DASTAP,DA-BELP
    Satsyialistychnaia peramoga,DASOCIL,DA-BELP
    Satsyialistychnaia pratsa,DASOCP,DA-BELP
    Shliakh satsyializma,DASOC,DA-BELP
    Shliakh satsyializma,DASOCI,DA-BELP
    Stalinets,DASTALC,DA-BELP
    Stalinskaia pra_da,DASTAL,DA-BELP
    Stalinski shliakh,DASTAS,DA-BELP
    Stalinski stsiag,DASTALS,DA-BELP
    Trybuna kalgasnika,DATRIB,DA-BELP
    Za Satsyialistychnuiu Radzimu,DASOCR,DA-BELP
    Za bal'shavitskiia kalgasy,DAZAB,DA-BELP
    Za svabodu,DAZAS,DA-BELP
    Zaria,DAZAR,DA-BELP
    Prapor peremogy,CHEPRPE,DA-ChNC
    Tribuna Energetika,CHETREN,DA-ChNC
    Trybuna pratsi,CHETRPR,DA-ChNC
    Boevoe znamia Donbassa,BZD,DA-DNR
    Boevoi listok Novorossii,BLN,DA-DNR
    Donetsk vechernii,DOV,DA-DNR
    Edinstvo,EDN,DA-DNR
    Nasha gazeta,NGZ,DA-DNR
    Novorossiia,NOR,DA-DNR
    Vostochnyi Donbass,VOD,DA-DNR
    XXI vek,XXI,DA-DNR
    Zaria Donbassa,ZDB,DA-DNR
    Zhizn' Luganska,ZLU,DA-DNR
    Gudok,DAGUDK14,DA-GUDO
    Argumenty i fakty,AIF,"UDB-COM, DA-AIF, UDB-COM+, UDB-IND"
    Izvestiia ,IZVMF,DA-IZV
    Literaturnaia Gazeta ,LGAMF,DA-LG
    Jiefangjun Bao  ,PDN,DA-PLA
    Kavkaz  ,KVZ,DA-KVZ
    Nedelia ,DANED,DA-NED
    Rossiiskaia gazeta ,DARGA,DA-RG
    "T.I.S.E.P. Reporter, The ",TIS,DA-TISEP
    Za Vozvrashchenie na Rodinu ,ZVAR,DA-ZVR
    Current Digest of the Chinese Press ,CDC,"UDB-EVP, UDB-IND, DA-CDCP"
    Current Digest of the Russian Press ,DSP,"	UDB-EVP, EVP-CD, UDB-IND, DA-CDRP "
    ,,
    DIGITAL ARCHIVES. JOURNALS�,,
    Title,Publication Acronym,Database Acronym
    Ogonek,,DA-OGN
    Ogonek (St. Petersburg),,DA-OGN-SP
    NewsNet,NEW,DA-NN
    Eksport Vooruzhenii ,DAEV,DA-EXP
    Evening Bulletin ,EBMF,
    Far Eastern Affairs ,NFEA,DA-FEA
    "Geography, Environment, Sustainability ",GES,DA-GES
    Illiustrirovannaia Rossiia  ,,DA-IR
    International Affairs ,IAF,DA-IA
    Kino-fot ,KIFO,DA-KFO
    Kino-zhurnal A.R.K. ,KINO,DA-KIN
    Krasnyi Arkhiv ,KRAR,DA-KAR
    Krokodil ,,DA-KRO
    Military Thought ,MTH,DA-MLT
    Moscow Defense Brief ,MDB,DA-MDB
    Muslims of the Soviet East ,MUSL,DA-MUS
    Problemy Kitaia ,DAPK,DA-PK
    Russkaia Literatura ,RLIA,DA-RLI
    Slaviane ,DASL,DA-SL
    Sovetskoe Zdravookhranenie ,DASZD,DA-SZD
    Soviet Woman ,SOW,DA-SW
    The Warsaw Pact Journal ,DAWPJ,DA-WPJ
    Vestnik Evropy ,VEV,DA-VE
    Voprosy Literatury ,VOL,DA-VL
    Iskusstvo Kino ,IKKMF,DA-IK
    Proletarskoe kino,IKKPMF,DA-IK
    Sovetskoe kino,IKKSMF,DA-IK
    LEF ,DALEF,DA-LEF
    Novyi LEF,DANLEF,DA-LEF
    Niva  ,NIVB,DA-NIVA
    Dlia Detei,DDT,DA-NIVA
    Russkii Arkhiv  ,RSA,DA-RSA
    Russkii Arkhiv: predmetnaia rospis',RSAPRR,DA-RSA
    Katalog knig Chertkovskoi biblioteki,RSAKLG,DA-RSA
    Severnye tsvety,RSASEV,DA-RSA
    Stikhotvoreniia Vasiliia Andreevicha Zhukovskago,RSASTG,DA-RSA
    Zapiski Filipa Filipovicha Vigelia,RSAZPV,DA-RSA
    Voennaia Mysl' ,DAVSM,DA-VM
    Voennoe delo,VOEN,DA-VM
    Voennaia nauka i revoliutsiia,VOENR,DA-VM
    Voennaia mysl' i revoliutsiia,VOMIR,DA-VM
    Voina i revoliutsiia,VOIR,DA-VM
    Voprosy Istorii ,VPI,DA-VI
    Bor'ba klassov,BKL,DA-VI
    Istoricheskii zhurnal,ISJ,DA-VI
    Istorik-marksist,ISM,DA-VI
     */

    static {
        canonical = new HashMap<>();

        // This section for journals
        canonical.put("OGN","Ogonek");
        canonical.put("OGNSP","Ogonek (St. Petersburg)");
        canonical.put("NEW","NewsNet");
        canonical.put("NN","NewsNet");
        canonical.put("DAEV","Eksport Vooruzhenii");
        canonical.put("EBMF","Evening Bulletin");
        canonical.put("NFEA","Far Eastern Affairs");
        canonical.put("GES","Geography, Environment, Sustainability");
        canonical.put("IR","Illiustrirovannaia Rossiia");
        canonical.put("IAF","International Affairs");
        canonical.put("KIFO","Kino-fot");
        canonical.put("KINO","Kino-zhurnal A.R.K.");
        canonical.put("KRAR","Krasnyi Arkhiv");
        canonical.put("KRO","Krokodil");
        canonical.put("MTH","Military Thought");
        canonical.put("MDB","Moscow Defense Brief");
        canonical.put("MUSL","Muslims of the Soviet East");
        canonical.put("DAPK","Problemy Kitaia");
        canonical.put("RLIA","Russkaia Literatura");
        canonical.put("DASL","Slaviane");
        canonical.put("DASZD","Sovetskoe Zdravookhranenie");
        canonical.put("SOW","Soviet Woman");
        canonical.put("DAWPJ","The Warsaw Pact Journal");
        canonical.put("VEV","Vestnik Evropy");
        canonical.put("VOL","Voprosy Literatury");
        canonical.put("IKKMF","Iskusstvo Kino");
        canonical.put("IKKPMF","Proletarskoe kino");
        canonical.put("IKKSMF","Sovetskoe kino");
        canonical.put("DALEF","LEF");
        canonical.put("DANLEF","Novyi LEF");
        canonical.put("NIVB","Niva ");
        canonical.put("DDT","Dlia Detei");
        canonical.put("RSA","Russkii Arkhiv ");
        canonical.put("RSAPRR","Russkii Arkhiv: predmetnaia rospis'");
        canonical.put("RSAKLG","Katalog knig Chertkovskoi biblioteki");
        canonical.put("RSASEV","Severnye tsvety");
        canonical.put("RSASTG","Stikhotvoreniia Vasiliia Andreevicha Zhukovskago");
        canonical.put("RSAZPV","Zapiski Filipa Filipovicha Vigelia");
        canonical.put("DAVSM","Voennaia Mysl'");
        canonical.put("VOEN","Voennoe delo");
        canonical.put("VOENR","Voennaia nauka i revoliutsiia");
        canonical.put("VOMIR","Voennaia mysl' i revoliutsiia");
        canonical.put("VOIR","Voina i revoliutsiia");
        canonical.put("VPI","Voprosy Istorii");
        canonical.put("BKL","Bor'ba klassov");
        canonical.put("ISJ","Istoricheskii zhurnal");
        canonical.put("ISM","Istorik-marksist");

        // This section is for newspapers
        ////////canonical.put("","DIGITAL ARCHIVES. NEWSPAPERS");
        canonical.put("Publication Acronym","Title");
        canonical.put("PRDMF","Pravda");
        canonical.put("PRDMFLP","Listok pravdy");
        canonical.put("PRDMFPE","Petrogradskaia pravda");
        canonical.put("PRDMFPA","Pravda. St. Petersburg ");
        canonical.put("PRDMFPR","Pravda. Petrograd  ");
        canonical.put("PRDMFPV","Pravda (Petrogradskaia pravda)");
        canonical.put("PRDMFSD","Pravda (Sotsial-Demokrat)");
        canonical.put("PRDMFPT","Pravda truda");
        canonical.put("PRDMFPO","Proletarii");
        canonical.put("PRDMFPP","Proletarskaia pravda");
        canonical.put("PRDMFPU","Put' pravdy");
        canonical.put("PRDMFRB","Rabochaia pravda");
        canonical.put("PRDMFRA","Rabochii");
        canonical.put("PRDMFRS","Rabochii I soldat");
        canonical.put("PRDMFRP","Rabochii put'");
        canonical.put("PRDMFSP","Severnaia pravda");
        canonical.put("PRDMFTP","Trudovaia pravda");
        canonical.put("PRDMFZP","Za pravdu");
        canonical.put("SKULMF","Sovetskaia kul'tura");
        canonical.put("KULMF","Kul'tura");
        canonical.put("LIKULMF","Literatura i iskusstvo");
        canonical.put("RKULMF","Rabochii i iskusstvo");
        canonical.put("SOVKULMF","Sovetskoe iskusstvo");
        canonical.put("MDN","Moscow News");
        canonical.put("MOSN","Moscow Daily News");
        canonical.put("DANRS","Novoe Russkoe Slovo");
        canonical.put("DARS","Russkoe Slovo");
        canonical.put("PRUK","Pravda Ukrainy");
        canonical.put("SOUK","Sovetskaia Ukraina");
        canonical.put("SKYR","Slovo Kyrgystana");
        canonical.put("BAPR","Batratskaia pravda");
        canonical.put("KRPU","Krest'ianskii put'");
        canonical.put("SOKI","Sovetskaia Kirgiziia");
        canonical.put("DAAVAN","Avangard");
        canonical.put("DABOLS","Bal'shavitski shliakh");
        canonical.put("DABOLG","Bal'shavitski stsiag");
        canonical.put("DACHER","Chyrvonaia zara");
        canonical.put("DAHLEB","Chyrvony khlebarob");
        canonical.put("DACHEG","Chyrvony stsiag");
        canonical.put("DAGOML","Gomel'skaia pra_da");
        canonical.put("DAKALT","Kalgasnaia trybuna");
        canonical.put("DAKALG","Kalgasnik Kapyl'shchyny");
        canonical.put("DAKALH","Kalgasny shliakh");
        canonical.put("DAKOM","Kamunar");
        canonical.put("DAKLIK","Klich Radzimy");
        canonical.put("DALEN","Leninski shliakh");
        canonical.put("DAMOM","My otomstim");
        canonical.put("DANOVA","Novaia veska");
        canonical.put("DAPLAM","Plamia");
        canonical.put("DASTAP","Po stalinskomu puti");
        canonical.put("DASOCIL","Satsyialistychnaia peramoga");
        canonical.put("DASOCP","Satsyialistychnaia pratsa");
        canonical.put("DASOC","Shliakh satsyializma");
        canonical.put("DASOCI","Shliakh satsyializma");
        canonical.put("DASTALC","Stalinets");
        canonical.put("DASTAL","Stalinskaia pra_da");
        canonical.put("DASTAS","Stalinski shliakh");
        canonical.put("DASTALS","Stalinski stsiag");
        canonical.put("DATRIB","Trybuna kalgasnika");
        canonical.put("DASOCR","Za Satsyialistychnuiu Radzimu");
        canonical.put("DAZAB","Za bal'shavitskiia kalgasy");
        canonical.put("DAZAS","Za svabodu");
        canonical.put("DAZAR","Zaria");
        canonical.put("CHEPRPE","Prapor peremogy");
        canonical.put("CHETREN","Tribuna Energetika");
        canonical.put("CHETRPR","Trybuna pratsi");
        canonical.put("BZD","Boevoe znamia Donbassa");
        canonical.put("BLN","Boevoi listok Novorossii");
        canonical.put("DOV","Donetsk vechernii");
        canonical.put("EDN","Edinstvo");
        canonical.put("NGZ","Nasha gazeta");
        canonical.put("NOR","Novorossiia");
        canonical.put("VOD","Vostochnyi Donbass");
        canonical.put("XXI","XXI vek");
        canonical.put("ZDB","Zaria Donbassa");
        canonical.put("ZLU","Zhizn' Luganska");
        canonical.put("DAGUDK14","Gudok");
        canonical.put("AIF","Argumenty i fakty");
        canonical.put("IZVMF","Izvestiia");
        canonical.put("LGAMF","Literaturnaia Gazeta");
        canonical.put("PDN","Jiefangjun Bao ");
        canonical.put("KVZ","Kavkaz ");
        canonical.put("DANED","Nedelia");
        canonical.put("DARGA","Rossiiskaia gazeta");
        canonical.put("TIS","T.I.S.E.P. Reporter, The");
        canonical.put("ZVAR","Za Vozvrashchenie na Rodinu");
        canonical.put("CDC","Current Digest of the Chinese Press");
        canonical.put("DSP","Current Digest of the Russian Press");
    }
}

