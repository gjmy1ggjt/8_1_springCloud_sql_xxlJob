package com.example.demo.utils;

import lombok.Data;

import java.io.Serializable;

@Data
public class PagingDto implements Serializable{
	private static final long serialVersionUID = -1595504837020950968L;
	
    public static final String CREATED_DATE = "createdDate";
    private Integer pageNo;
    private Integer pageSize;
    private String[] orderBy;
    private String[] order;


    public String toOrderSql(String tableAlias) {
        StringBuilder sb = new StringBuilder();

        if (this.order != null && this.order.length > 0) {
            sb.append(" order by ");

            for (int i = 0; i < this.order.length; i++) {
                // 替换成到数据库字段+order by 条件

                sb.append(tableAlias + "." + this.orderBy[i] + " ");
                sb.append(this.order[i] + " ");

                if (i < this.orderBy.length - 1) {
                    sb.append(",");
                }
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) {

        PagingDto pagingDto = new PagingDto();

        pagingDto.setOrderBy(new String[]{"createdDate"});

        pagingDto.setOrder(new String[]{"desc"});

        System.out.println(pagingDto.toOrderSql("t"));
    }

}
