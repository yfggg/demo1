package com.example.demo.entity;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author yangfan
 * @since 2022-01-14
 */
@Getter
@Setter
public class War implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String accountId;

    private String war;


}
