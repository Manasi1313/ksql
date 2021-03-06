/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.serde.tracked;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Deserializer injected to track what topics a serde is used for.
 */
final class TrackedDeserializer<T> implements Deserializer<T> {

  private final Deserializer<T> inner;
  private final TrackedCallback callback;
  private Boolean key;

  TrackedDeserializer(final Deserializer<T> inner, final TrackedCallback callback) {
    this.inner = requireNonNull(inner, "inner");
    this.callback = requireNonNull(callback, "callback");
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    inner.configure(configs, isKey);
    key = isKey;
  }

  @Override
  public T deserialize(final String topic, final byte[] data) {
    track(topic);
    return inner.deserialize(topic, data);
  }

  @Override
  public T deserialize(final String topic, final Headers headers, final byte[] data) {
    track(topic);
    return inner.deserialize(topic, headers, data);
  }

  @Override
  public void close() {
    inner.close();
  }

  private void track(final String topicName) {
    if (key == null) {
      throw new IllegalStateException("Configure not called");
    }

    callback.accept(topicName, key);
  }
}
