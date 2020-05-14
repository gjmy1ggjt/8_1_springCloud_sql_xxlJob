package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.syst.Airline;
import com.yangshan.eship.author.entity.syst.AirlineFlightNo;
import com.yangshan.eship.author.entity.syst.BookingParty;
import com.yangshan.eship.author.service.syst.AirlineFlightNoServiceI;
import com.yangshan.eship.author.service.syst.AirlineServiceI;
import com.yangshan.eship.author.service.syst.BookingPartyServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 查询订舱公司
 *
 * @author: Kee.Li
 * @date: 2017/10/23 18:00
 */
@RestController("BookingPartyControllerInSalesLib")
@RequestMapping(Version.VERSION + "/bookingParties")
public class BookingPartyController {

    @Autowired
    private BookingPartyServiceI bookingPartyService;

    @Autowired
    private AirlineFlightNoServiceI airlineFlightNoService;

    @Autowired
    private AirlineServiceI airlineService;

    /**
     * 订舱公司查询
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/24 10:05
     */
    @RequestMapping(method = RequestMethod.GET)
    public DataGrid<BookingParty> list(String organizationId) {

        // 从session中获取组织id
        String orgId = SessionUtils.getOrganizationId();

        List<BookingParty> bookingPartyList = bookingPartyService.findByOrgId(orgId);

        DataGrid<BookingParty> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setCode(HttpStatus.OK.value() + "");
        dataGrid.setMsg(HttpStatus.OK.name());
        dataGrid.setRows(bookingPartyList);
        dataGrid.setTotal(bookingPartyList == null ? 0 : bookingPartyList.size());

        return dataGrid;

    }

//    /**
//     * 订舱公司的航空公司航班查询
//     *
//     * @author: Kee.Li
//     * @date: 2017/10/24 10:06
//     */
//    @RequestMapping(value = "/{bookingPartyId}/flightNos", method = RequestMethod.GET)
//    public DataGrid<AirlineFlightNo> list(@PathVariable("bookingPartyId") String bookingPartyId, String organizationId) {
//
//        // 从session中获取组织id
//        organizationId = SessionUtils.getOrganizationId();
//
//        return airlineFlightNoService.findByBookingParty(bookingPartyId, organizationId);
//    }


    /**
     * 查询订舱公司下面所有的航空公司编码
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/9 10:02
     */
    @RequestMapping(value = "/{bookingPartyId}/airlineCompanies", method = RequestMethod.GET)
    public DataGrid<String> findAirlineCompanyNos(@PathVariable("bookingPartyId") String bookingPartyId) {

        String organizationId = SessionUtils.getOrganizationId();
        return airlineFlightNoService.findAirlineCompanyNos(bookingPartyId, organizationId);
    }

    /**
     * 查询一个组织下，一个航空公司下的所有航班号
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/9 10:08
     */
    @RequestMapping(value = "/{airlineCompanyNo}/flightNos", method = RequestMethod.GET)
    public DataGrid<AirlineFlightNo> findAirlineCompanyFlightNos(@PathVariable("airlineCompanyNo") String airlineCompanyNo) {

        String organizationId = SessionUtils.getOrganizationId();

        return airlineFlightNoService.findAirlineCompanyFlightNos(organizationId, airlineCompanyNo);
    }

    /**
     * 查询一个组织下的所有航空公司编码
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/9 14:53
     */
    @RequestMapping(value = "/airlineCompanies", method = RequestMethod.GET)
    DataGrid<String> findAirlineCompanyNos() {

        String organizationId = SessionUtils.getOrganizationId();

        return airlineFlightNoService.findAirlineCompanyNos(organizationId);
    }

    /**
     * 查询订舱公司的航空公司
     *
     * @param bookingPartyId
     * @return
     */
    @RequestMapping(value = "/{bookingPartyId}/airlines", method = RequestMethod.GET)
    DataGrid<Airline> findByBookingParty(@PathVariable("bookingPartyId") String bookingPartyId) {

        List<Airline> airlines = airlineService.findByBookingParty(bookingPartyId);
        if(airlines != null && !airlines.isEmpty()){
            return new DataGrid<>(true,airlines);
        }

        return new DataGrid<>(true,0);
    }

    /**
     * 查询一个组织下的所有航空公司编码
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/9 14:53
     */
    @RequestMapping(value = "/airlines", method = RequestMethod.GET)
    DataGrid<Airline> findAirlines() {

        List<Airline> airlines = airlineService.findByOrgId(SessionUtils.getOrganizationId());
        if(airlines != null && !airlines.isEmpty()){
            return new DataGrid<>(true,airlines);
        }

        return new DataGrid<>(true,0);
    }

}
