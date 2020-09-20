/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.writer.execution;

import org.apache.hadoop.conf.Configuration;
import org.apache.hudi.common.table.timeline.HoodieInstant;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.exception.HoodieException;
import org.apache.hudi.writer.client.HoodieWriteClient;
import org.apache.hudi.writer.client.WriteStatus;
import org.apache.hudi.writer.common.HoodieWriteOutput;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Run one round of compaction.
 */
public class Compactor implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LogManager.getLogger(Compactor.class);

  private transient HoodieWriteClient compactionClient;
  private transient Configuration hadoopConf;

  public Compactor(HoodieWriteClient compactionClient, Configuration hadoopConf) {
    this.hadoopConf = hadoopConf;
    this.compactionClient = compactionClient;
  }

  public void compact(HoodieInstant instant) throws IOException {
    LOG.info("Compactor executing compaction " + instant);
    HoodieWriteOutput<List<WriteStatus>> res = compactionClient.compact(instant.getTimestamp());
    long numWriteErrors = res.getOutput().stream().filter(WriteStatus::hasErrors).count();
    if (numWriteErrors != 0) {
      // We treat even a single error in compaction as fatal
      LOG.error("Compaction for instant (" + instant + ") failed with write errors. Errors :" + numWriteErrors);
      throw new HoodieException(
          "Compaction for instant (" + instant + ") failed with write errors. Errors :" + numWriteErrors);
    }
    // Commit compaction
    compactionClient.commitCompaction(instant.getTimestamp(), res, Option.empty());
  }
}