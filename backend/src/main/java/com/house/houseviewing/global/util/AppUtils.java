package com.house.houseviewing.global.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

public class AppUtils {

    public static URI createLocation(String path, Long id){
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path)
                .buildAndExpand(id)
                .toUri();
    }
}
