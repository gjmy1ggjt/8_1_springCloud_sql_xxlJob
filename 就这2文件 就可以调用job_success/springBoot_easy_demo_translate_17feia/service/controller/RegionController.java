package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.syst.AirlineFlightNoServiceI;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 出发地、中转地、目的地
 *
 * @author: Kee.Li
 * @date: 2017/10/24 9:36
 */
@RestController("RegionControllerInSalesLib")
@RequestMapping(Version.VERSION + "/region")
public class RegionController {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(RegionController.class);

    @Autowired
    private RegionServiceI regionService;

    @Autowired
    private AirlineFlightNoServiceI airlineFlightNoService;

    /**
     * 查询所有主单出发地
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/24 10:10
     */
    @RequestMapping(value = "/origins", method = RequestMethod.GET)
    public DataGrid<Region> listOrigins() {

        String organizationId = SessionUtils.getOrganizationId();

        DataGrid<Region> dataGrid = new DataGrid<>();
        DataGrid<String> origins = airlineFlightNoService.findOrigins(organizationId);

        setRegionDataGrid(dataGrid, origins);
        dataGrid.setFlag(true);
        return dataGrid;
    }


    /**
     * 查询主单目的地
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/24 10:13
     */
    @RequestMapping(value = "/destinations", method = RequestMethod.GET)
    public DataGrid<Region> listDestinations() {

        String organizationId = SessionUtils.getOrganizationId();

        DataGrid<Region> dataGrid = new DataGrid<>();
        DataGrid<String> origins = airlineFlightNoService.findDestinations(organizationId);

        setRegionDataGrid(dataGrid, origins);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    private void setRegionDataGrid(DataGrid<Region> dataGrid, DataGrid<String> origins) {
        if (origins.getTotal() > 0) {
            for (int i = 0; i < origins.getTotal(); i++) {
                String airportCode = origins.getRows().get(i);
                Region region = new Region();
                region.setTransit(true);
                region.setAirportCode(airportCode);
                dataGrid.getRows().add(region);
            }
        }
    }

    /**
     * 获取所有的中转地
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/24 15:45
     */
    @RequestMapping(value = "/transits", method = RequestMethod.GET)
    public DataGrid<Region> listTransits() {

        List<Region> regions = regionService.findTransits();

        DataGrid<Region> dataGrid = getRegionDataGrid(regions);

        return dataGrid;
    }

    /**
     * 查询产品目的地
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/24 10:13
     */
    @RequestMapping(value = "/products/destinations", method = RequestMethod.GET)
    public DataGrid<Region> listProductDestinations() {

        List<Region> regions = regionService.findDestinations();

        DataGrid<Region> dataGrid = getRegionDataGrid(regions);

        return dataGrid;
    }

    private DataGrid<Region> getRegionDataGrid(List<Region> regions) {
        DataGrid<Region> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setCode(HttpStatus.OK.value() + "");
        dataGrid.setMsg(HttpStatus.OK.name());
        dataGrid.setRows(regions);
        dataGrid.setTotal(regions != null ? regions.size() : 0);
        return dataGrid;
    }

    @RequestMapping(value = "/findOriginList", method = RequestMethod.GET)
    public DataGrid<Region> findOriginList() {
        DataGrid dataGrid = new DataGrid<Region>();
        try {
            Map<String, String> regions = regionService.findOriginList();

            dataGrid.setFlag(true);
            dataGrid.setObj(regions);
            dataGrid.setMsg("获取成功");
        } catch (Exception e) {
            dataGrid.setMsg("获取失败");
            logger.info(e.getMessage(), e);
        }

        return dataGrid;
    }

    @RequestMapping(value = "/findIdOriginList", method = RequestMethod.GET)
    public DataGrid<Region> findIdOriginList() {
        DataGrid dataGrid = new DataGrid<Region>();
        try {
            Map<String, String> regions = regionService.findIdOriginList();

            dataGrid.setFlag(true);
            dataGrid.setObj(regions);
            dataGrid.setMsg("获取成功");
        } catch (Exception e) {
            dataGrid.setMsg("获取失败");
            logger.info(e.getMessage(), e);
        }

        return dataGrid;
    }
}
