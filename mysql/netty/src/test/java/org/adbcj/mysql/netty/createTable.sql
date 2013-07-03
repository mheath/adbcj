create schema unit_test;
use unit_test;
CREATE TABLE `type_test` (
  `varcharr` varchar(255) DEFAULT NULL,
  `charr` char(255) DEFAULT NULL,
  `blobr` blob,
  `integerr` int(11) DEFAULT NULL,
  `integerr_unsigned` int(11) UNSIGNED  DEFAULT NULL,
  `tinyintr` tinyint(4) DEFAULT NULL,
  `tinyintr_unsigned` tinyint(4) UNSIGNED DEFAULT NULL,
  `smallintr` smallint(6) DEFAULT NULL,
  `smallintr_unsigned` smallint(6) UNSIGNED DEFAULT NULL,
  `mediumintr` mediumint(9) DEFAULT NULL,
  `mediumintr_unsigned`  mediumint(9) UNSIGNED DEFAULT NULL,
  `bitr` bit(1) DEFAULT NULL,
  `bigintr` bigint(20) DEFAULT NULL,
  `bigintr_unsigned` bigint(20) UNSIGNED DEFAULT NULL,
  `floatr` float DEFAULT NULL,
  `doubler` double DEFAULT NULL,
  `decimalr` decimal(10,0) DEFAULT NULL,
  `dater` date DEFAULT NULL,
  `timer` time DEFAULT NULL,
  `datetimer` datetime DEFAULT NULL,
  `timestampr` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `yearr` year(4) DEFAULT NULL,
  `pk` int(11) NOT NULL,
  PRIMARY KEY (`pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;