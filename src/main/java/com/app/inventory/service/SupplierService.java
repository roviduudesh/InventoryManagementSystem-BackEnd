package com.app.inventory.service;

import com.app.inventory.dto.IdNameDto;
import com.app.inventory.dto.SupplierDto;
import com.app.inventory.dto.ResponseDto;
import com.app.inventory.dto.ValidateDto;
import com.app.inventory.model.Stock;
import com.app.inventory.model.key.SupplierContactKey;
import com.app.inventory.model.Supplier;
import com.app.inventory.model.SupplierContact;
import com.app.inventory.repository.StockRepository;
import com.app.inventory.repository.SupplierContactRepository;
import com.app.inventory.repository.SupplierRepository;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.app.inventory.service.Common.*;

@Service
@Data
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierContactRepository supplierContactRepository;
    private final StockRepository stockRepository;

    public ResponseEntity<?> getSupplierList(){
        try {
            List<Supplier> supplierList = supplierRepository.findAllByOrderByNameAsc();
            List<SupplierDto> supplierDtoList = new ArrayList<>();
            SupplierDto supplierDto;
            String contact;
            for (Supplier supplier : supplierList) {
                supplierDto = new SupplierDto();
                supplierDto.setId(supplier.getId());
                supplierDto.setSupName(supplier.getName());
                supplierDto.setAddress1(supplier.getAddress1());
                supplierDto.setAddress2(supplier.getAddress2());
                supplierDto.setAddress3(supplier.getAddress3());
                supplierDto.setEmail(supplier.getEmail());
                contact = "";
                if(supplier.getSupplierContactList().size() > 0) {
                    for (SupplierContact supplierContact : supplier.getSupplierContactList()) {
                        contact = contact + supplierContact.getSupConKey().getContact() + ", ";
                    }
                    supplierDto.setContact(contact.substring(0, contact.length() - 2));
                }
                supplierDtoList.add(supplierDto);
            }
            return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, supplierDtoList), HttpStatus.OK);
        } catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), EXCEPTION, null), HttpStatus.EXPECTATION_FAILED);
        }
    }

    ValidateDto validateSupplier(SupplierDto supplierDto){
        ValidateDto validateDto = new ValidateDto();
        Optional<Supplier> supplierOptional;
        Optional<String> invalidContact = Optional.empty();
        boolean isValid = true;
        String message = null;
        List<Supplier> supplierList = supplierRepository.findAll();

        supplierOptional = supplierList.stream()
                .filter(s -> s.getId() != supplierDto.getId() && s.getName().equalsIgnoreCase(supplierDto.getSupName().trim()))
                .findFirst();
        if(supplierDto.getContact() != null) {
            List<String> contactList = Arrays.asList(supplierDto.getContact().split("\\s*,\\s*"));
            invalidContact = contactList.stream()
                    .filter(c -> !c.isEmpty() && c.length() != 10)
                    .findAny();
        }
        if(invalidContact.isPresent()){
            isValid = false;
            message = "Invalid Contact Number";
        } else if(supplierOptional.isPresent()){
            isValid = false;
            message = "Name Exists";
        } else {
            supplierOptional = supplierList.stream()
                    .filter(s -> s.getId() != supplierDto.getId() && !s.getEmail().isEmpty() && s.getEmail().equalsIgnoreCase(supplierDto.getEmail().trim()))
                    .findFirst();
            if (supplierOptional.isPresent()) {
                isValid = false;
                message = "Email Exists";
            }
        }
        validateDto.setValid(isValid);
        validateDto.setMessage(message);
        return validateDto;
    }

    public ResponseEntity<?> createNewSupplier(SupplierDto supplierDto) {
        try {
            ValidateDto validateDto = validateSupplier(supplierDto);

            if(!validateDto.isValid()){
                return new ResponseEntity<>(new ResponseDto(HttpStatus.NOT_ACCEPTABLE.value(), validateDto.getMessage(), null), HttpStatus.NOT_ACCEPTABLE);
            } else {
                Supplier supplier = new Supplier();
                supplier.setName(supplierDto.getSupName());
                supplier.setAddress1(supplierDto.getAddress1());
                supplier.setAddress2(supplierDto.getAddress2());
                supplier.setAddress3(supplierDto.getAddress3());
                supplier.setCreatedDate(LocalDateTime.now());
                supplier.setEmail(supplierDto.getEmail());
                supplierRepository.save(supplier);

                supplier = supplierRepository.findTopByOrderByIdDesc();
                if(supplierDto.getContact() != null) {
                    setSupplierContact(supplier.getId(), supplierDto.getContact(), "insert");
                }
                return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, null), HttpStatus.OK);
            }
        } catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, null), HttpStatus.EXPECTATION_FAILED);
        }
    }

    void setSupplierContact(int supplierId, String contact, String process){
        List<SupplierContact> supplierContactList = new ArrayList<>();
        SupplierContact supplierContact;
        SupplierContactKey supplierContactKey;
        List<String> contactList = Arrays.asList(contact.split("\\s*,\\s*"));

        for (String contactNumber : contactList) {
            supplierContactKey = new SupplierContactKey();
            supplierContactKey.setSupplierId(supplierId);
            supplierContactKey.setContact(contactNumber);

            supplierContact = new SupplierContact();
            supplierContact.setSupConKey(supplierContactKey);
            supplierContactList.add(supplierContact);
        }
        if(process.equalsIgnoreCase("update")){
            supplierContactRepository.deleteBySupConKey_SupplierId(supplierId);
        }
        supplierContactRepository.saveAll(supplierContactList);
    }

    public ResponseEntity<?> updateSupplier(int supplierId, SupplierDto supplierDto) {
        try {
            ValidateDto validateDto = validateSupplier(supplierDto);

            if(!validateDto.isValid()){
                return new ResponseEntity<>(new ResponseDto(HttpStatus.NOT_ACCEPTABLE.value(), validateDto.getMessage(), null), HttpStatus.NOT_ACCEPTABLE);
            } else {
                Optional<Supplier> supplierOptional = supplierRepository.findById(supplierId);
                if (supplierOptional.isPresent()) {
                    Supplier supplier = supplierOptional.get();
                    supplier.setName(supplierDto.getSupName());
                    supplier.setAddress1(supplierDto.getAddress1());
                    supplier.setAddress2(supplierDto.getAddress2());
                    supplier.setAddress3(supplierDto.getAddress3());
                    supplier.setEmail(supplierDto.getEmail());
                    supplierRepository.save(supplier);

                    if(supplierDto.getContact() != null) {
                        setSupplierContact(supplierId, supplierDto.getContact(), "update");
                    }
                    return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, null), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new ResponseDto(HttpStatus.NO_CONTENT.value(), "Supplier Not Found", null), HttpStatus.NO_CONTENT);
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(new ResponseDto(HttpStatus.EXPECTATION_FAILED.value(), EXCEPTION, null), HttpStatus.EXPECTATION_FAILED);
        }
    }

    public ResponseEntity<?> deleteSupplier(int supplierId) {
        try {
            Optional<Supplier> supplierOptional = supplierRepository.findById(supplierId);
            if (supplierOptional.isPresent()) {
                List<Stock> optionalStockList = stockRepository.findAllBySupplier(supplierOptional.get());
                if (optionalStockList.isEmpty()) {
                    supplierRepository.deleteSupplier(supplierId);
                    return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, null), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new ResponseDto(HttpStatus.NOT_ACCEPTABLE.value(), "Supplier has Stocks", null), HttpStatus.NOT_ACCEPTABLE);
                }
            } else{
                return new ResponseEntity<>(new ResponseDto(HttpStatus.NO_CONTENT.value(), "Supplier Not Found", null), HttpStatus.NO_CONTENT);
            }
        } catch (Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(new ResponseDto(HttpStatus.EXPECTATION_FAILED.value(), EXCEPTION, null), HttpStatus.EXPECTATION_FAILED);
        }
    }

    public ResponseEntity<?> getSupIdNameList(){
        ResponseDto responseDto = new ResponseDto();
        try {
            List<Supplier> supplierList = supplierRepository.findAllByOrderByNameAsc();
            List<IdNameDto> idNameDtoList = new ArrayList<>();
            IdNameDto idNameDto;
            for (Supplier supplier : supplierList) {
                idNameDto = new IdNameDto();
                idNameDto.setId(supplier.getId());
                idNameDto.setName(supplier.getName());

                idNameDtoList.add(idNameDto);
            }
            return new ResponseEntity<>(new ResponseDto(HttpStatus.OK.value(), SUCCESS, idNameDtoList), HttpStatus.OK);
        } catch (Exception ex){
            return new ResponseEntity<>(new ResponseDto(HttpStatus.EXPECTATION_FAILED.value(), EXCEPTION, null), HttpStatus.EXPECTATION_FAILED);
        }
    }
}
