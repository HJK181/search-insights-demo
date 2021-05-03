package com.example.searchinsightsdemo.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class StorageConfiguration {

	private String uploadDir = "/tmp/upload";

}
