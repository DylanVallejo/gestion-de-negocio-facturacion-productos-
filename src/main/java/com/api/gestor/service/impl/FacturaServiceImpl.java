package com.api.gestor.service.impl;

import com.api.gestor.constantes.FacturaConstantes;
import com.api.gestor.dao.FacturaDAO;
import com.api.gestor.pojo.Factura;
import com.api.gestor.security.jwt.JwtFilter;
import com.api.gestor.service.FacturaService;
import com.api.gestor.util.FacturaUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.util.Map;
import java.util.stream.Stream;


@Slf4j
@Service
public class FacturaServiceImpl implements FacturaService {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private FacturaDAO facturaDAO;

//generando el reporte
    @Override
    public ResponseEntity<String> generateReport(Map<String, Object> requestMap) {
        log.info("dentro del generador de reportes");
        try {
            String fileName;
            if(validateRequestMap(requestMap)){
                if (requestMap.containsKey("isGenerated") && !(Boolean)requestMap.get("isGenerated")){
                    fileName = (String)  requestMap.get("uuid");
                }else {
                    fileName = FacturaUtils.getUUID();
                    requestMap.put("uuid", fileName);
                    insertarFactura(requestMap);
                }
                String data = "Nombre : " + requestMap.get("nombre") +
                        "\nNumero de contacto: " + requestMap.get("numeroContacto") +
                        "\nEmail : " + requestMap.get("email") +
                        "\nMetodo de pago" + requestMap.get("metodoPago");

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(FacturaConstantes.STORE_LOCATION+"\\"+fileName+".pdf"));

                document.open();
                setRectangleInPdf(document);
                Paragraph paragraphHeader = new Paragraph("Gestion de categorias y productos", getFont("Header"));
                paragraphHeader.setAlignment(Element.ALIGN_CENTER);
                paragraphHeader.add(paragraphHeader);

                PdfPTable pdfPTable = new PdfPTable(5);
                pdfPTable.setWidthPercentage(100);
                addTableHeader(pdfPTable);

                JSONArray jsonArray = FacturaUtils.getJsonArrayFromString((String) requestMap.get("productoDetalles"));
                for (int i =0; i< jsonArray.length(); i++){
                    addRows(pdfPTable, FacturaUtils.getMapFromJson(jsonArray.toString(i)));
                }
                document.add(pdfPTable);

                Paragraph footer = new Paragraph("Total : "+ requestMap.get("total") + "\n" + " Gracias por visitarnos, vuelva pronto", getFont("Data"));
                document.add(footer);

                document.close();

                return new ResponseEntity<>("{\"uuid\":\""+fileName+"\"}", HttpStatus.OK);
            }

            return FacturaUtils.getResponseEntity("Datos requeridos no encontrados", HttpStatus.BAD_REQUEST);
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return FacturaUtils.getResponseEntity(FacturaConstantes.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // creando filas
    private void addRows(PdfPTable pdfPTable, Map<String, Object> data){
        log.info("dentro de addRows");
        pdfPTable.addCell((String) data.get("nombre"));
        pdfPTable.addCell((String) data.get("categoria"));
        pdfPTable.addCell((String) data.get("cantidad"));
        pdfPTable.addCell(Double.toString( (Double) data.get("precio")) );
        pdfPTable.addCell(Double.toString( (Double) data.get("total")) );

    }

    //crear rectangulo en pdf
    private void setRectangleInPdf(Document document) throws DocumentException {
        log.info("dentro de setRectangleInPdf");
        Rectangle rectangle = new Rectangle(577, 825, 18 , 15);
        rectangle.enableBorderSide(1);
        rectangle.enableBorderSide(2);
        rectangle.enableBorderSide(4);
        rectangle.enableBorderSide(8);
        rectangle.setBorderColor(BaseColor.BLACK);
        rectangle.setBorderWidth(1);
        document.add(rectangle);

    }

    // establecer fuente
    private Font getFont(String type){
        log.info("Deentro de getFont");
        switch (type){
            case "Header":
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 18,BaseColor.BLACK);
                headerFont.setStyle(Font.BOLD);
                return headerFont;
            case "Data":
                Font dataFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11,BaseColor.BLACK);
                dataFont.setStyle(Font.BOLD);
                return dataFont;
            default:
                return new Font();
        }
    }

    //insertar(crear) una factura y validar que la factura este correcta

    private void insertarFactura(Map<String, Object>requestMap){
        try {
            Factura factura = new Factura();
            factura.setUuid((String) requestMap.get("uuid"));
            factura.setNombre((String) requestMap.get("nombre"));
            factura.setEmail((String) requestMap.get("email"));
            factura.setNumeroContacto((String) requestMap.get("numeroContacto"));
            factura.setMetodoPago((String) requestMap.get("metodoPago"));
            factura.setTotal(Integer.parseInt((String) requestMap.get("total")));
            factura.setProductoDetalles((String) requestMap.get("productoDetalles"));
            factura.setCreatedBy(jwtFilter.getCurrentUser());
            facturaDAO.save(factura);


        }catch (Exception exception){
            exception.printStackTrace();
        }

    }


    //validar datos en factura
    private boolean validateRequestMap(Map<String, Object> requestMap){
        return requestMap.containsKey("numeroContacto")
                && requestMap.containsKey("numeroContacto")
                && requestMap.containsKey("email")
                && requestMap.containsKey("metodoPago")
                && requestMap.containsKey("productoDetalles")
                && requestMap.containsKey("total");

    }

    //creando una cabecera para la tabla


    private void addTableHeader(PdfPTable pdfPTable){
        log.info("Dentro del addTableHeader");
        Stream.of("Nombre", " Categoria", "Precio", "Sub Total")
                .forEach(columnTitle->{
//                    se puede ahcer con jasperreport tambien
                    PdfPCell pdfPCell = new PdfPCell();
                    pdfPCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    pdfPCell.setBorderWidth(2);
//                    aqui setea los titulos
                    pdfPCell.setPhrase(new Phrase(columnTitle));
                    pdfPCell.setBackgroundColor(BaseColor.YELLOW);
                    pdfPCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    pdfPTable.addCell(pdfPCell);

                });
    }



}
