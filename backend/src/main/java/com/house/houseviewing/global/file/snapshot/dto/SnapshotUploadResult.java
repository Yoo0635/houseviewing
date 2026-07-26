package com.house.houseviewing.global.file.snapshot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @AllArgsConstructor
@Builder @NoArgsConstructor
public class SnapshotUploadResult {

    private String snapshotName;

    private String snapshotUrl;

    private Long snapshotSizeBytes;
}
