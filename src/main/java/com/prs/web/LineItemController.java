package com.prs.web;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import com.prs.business.JsonResponse;
import com.prs.business.LineItem;
import com.prs.business.Product;
import com.prs.business.Request;
import com.prs.db.LineItemRepository;
import com.prs.db.RequestRepository;

@CrossOrigin()
@RestController
@RequestMapping("/line-items")
public class LineItemController {
	@Autowired
	private LineItemRepository lineItemRepo;
	
	@Autowired 
	private RequestRepository requestRepo;

	@GetMapping("/")
	public JsonResponse list() {
		JsonResponse jr = null;
		try {
			List<LineItem> lineItems = lineItemRepo.findAll();
			if (lineItems.size() > 0) {
				jr = JsonResponse.getInstance(lineItems);
			} else {
				jr = JsonResponse.getErrorInstance("No line items found.");
			}
		} catch (Exception e) {
			jr = JsonResponse.getInstance(e);
		}
		return jr;
	}

	@GetMapping("/{id}")
	public JsonResponse get(@PathVariable int id) {
		JsonResponse jr = null;
		try {
		Optional<LineItem> lineItem = lineItemRepo.findById(id);
		if (lineItem.isPresent()) {
			jr = JsonResponse.getInstance(lineItem.get());
		} else {
			jr = JsonResponse.getErrorInstance("No line item found for ID: " + id);
		}
		} catch (Exception e) {
			jr = JsonResponse.getInstance(e);
		}
		return jr;
	}
	
	@GetMapping("/lines-for-pr/{id}")
	public JsonResponse getLineItemsForRequest(@PathVariable int id) {
		JsonResponse jr = null;
		Optional<Request> requests = requestRepo.findById(id);
		List<LineItem> lineItems = lineItemRepo.findAllByRequest(requests);

		if (!lineItems.isEmpty()) {
			jr = JsonResponse.getInstance(lineItems);
		} else {
			jr = JsonResponse.getErrorInstance("No line items found for request id: " + id);
		}
		return jr;
	}

	@PostMapping("/")
	public JsonResponse createLineItem(@RequestBody LineItem li) {
		JsonResponse jr = null;
		Request request = li.getRequestId();
		double totalBackup = request.getTotal();
		try {
			li = lineItemRepo.save(li);
			request.setTotal(recalculateTotal(request));
			requestRepo.save(request);
			jr = JsonResponse.getInstance(li);
		} catch (DataIntegrityViolationException dive) {
			jr = JsonResponse.getErrorInstance(dive.getRootCause().getMessage());
			dive.printStackTrace();
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error creating line item: " + e.getMessage());
			e.printStackTrace();
			request.setTotal(totalBackup);
		}
		return jr;
	}

	@PutMapping("/")
	public JsonResponse updateLineItem(@RequestBody LineItem li) {
		JsonResponse jr = null;
		Request request = li.getRequestId();
		double totalBackup = request.getTotal();
		try {
			li = lineItemRepo.save(li);
			request.setTotal(recalculateTotal(request));
			requestRepo.save(request);
			jr = JsonResponse.getInstance(li);
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error updating line item: " + e.getMessage());
			e.printStackTrace();
			request.setTotal(totalBackup);
		}
		return jr;
	}

	@DeleteMapping("/{id}")
	public JsonResponse deleteLineItem(@PathVariable int id) {
		JsonResponse jr = null;
		LineItem li = lineItemRepo.findRequestById(id);
		Request reqId = li.getRequestId();
		Request request = requestRepo.findAllById(reqId.getId());
		double totalBackup = request.getTotal();
		try {
			lineItemRepo.deleteById(id);
			jr = JsonResponse.getInstance("Line item with ID: " + id + " deleted successfully.");
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error deleting line item: " + e.getMessage());
			e.printStackTrace();
			request.setTotal(totalBackup);
		}
		return jr;
	}
	
	public double recalculateTotal(Request r) {
		
		List<LineItem> lineItems = lineItemRepo.findAllByRequest(r);
		double total = 0.0;
		
		for (LineItem lineItem: lineItems) {
			Product product = lineItem.getProductId();
			total += product.getPrice() * lineItem.getQuantity();
		}
		
		return total;
	}
}
