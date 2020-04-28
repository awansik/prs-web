package com.prs.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import com.prs.business.JsonResponse;
import com.prs.business.Request;
import com.prs.business.User;
import com.prs.db.RequestRepository;
import com.prs.db.UserRepository;

@RestController
@RequestMapping("/requests")
public class RequestController {
	@Autowired
	private RequestRepository requestRepo;
	@Autowired
	private UserRepository userRepo;

	@GetMapping("/")
	public JsonResponse list() {
		JsonResponse jr = null;
		List<Request> request = requestRepo.findAll();
		if (request.size() > 0) {
			jr = JsonResponse.getInstance(request);
		} else {
			jr = JsonResponse.getErrorInstance("No requests found.");
		}
		return jr;
	}

	@GetMapping("/{id}")
	public JsonResponse get(@PathVariable int id) {
		JsonResponse jr = null;
		Optional<Request> request = requestRepo.findById(id);
		if (request.isPresent()) {
			jr = JsonResponse.getInstance(request.get());
		} else {
			jr = JsonResponse.getErrorInstance("No request found for ID: " + id);
		}
		return jr;
	}

	@PostMapping("/")
	public JsonResponse createRequest(@RequestBody Request r) {
		JsonResponse jr = null;
		try {
			r.setStatus("New");
			r.setSubmittedDate(LocalDateTime.now());
			r = requestRepo.save(r);
			jr = JsonResponse.getInstance(r);
		} catch (DataIntegrityViolationException dive) {
			jr = JsonResponse.getErrorInstance(dive.getRootCause().getMessage());
			dive.printStackTrace();
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error creating request: " + e.getMessage());
			e.printStackTrace();
		}
		return jr;
	}

	@PutMapping("/")
	public JsonResponse updateProduct(@RequestBody Request r) {
		JsonResponse jr = null;
		try {
			r = requestRepo.save(r);
			jr = JsonResponse.getInstance(r);
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error updating request: " + e.getMessage());
			e.printStackTrace();
		}
		return jr;
	}

	@DeleteMapping("/{id}")
	public JsonResponse deleteRequest(@PathVariable int id) {
		JsonResponse jr = null;
		try {
			requestRepo.deleteById(id);
			jr = JsonResponse.getInstance("Request with ID: " + id + " deleted successfully.");
		} catch (Exception e) {
			jr = JsonResponse.getErrorInstance("Error deleting request: " + e.getMessage());
			e.printStackTrace();
		}
		return jr;
	}

	@PutMapping("/submit-review")
	public JsonResponse submitRequestForReview(@RequestBody Request r) {
		JsonResponse jr = null;
		try {
			if (requestRepo.existsById(r.getId())) {
				if (r.getTotal() <= 50.00) {
					r.setStatus("Approved");
					r.setSubmittedDate(LocalDateTime.now());
				} else {
					r.setStatus("Review");
					r.setSubmittedDate(LocalDateTime.now());
				}
				jr = JsonResponse.getInstance(requestRepo.save(r));
			} else {
				jr = JsonResponse.getInstance(
						"PurchaseRequest ID: " + r.getId() + " does not exist and you are attempting to save it");
			}

		} catch (Exception e) {
			jr = JsonResponse.getInstance(e);
		}
		return jr;
	}
	
	@GetMapping("/list-review/{id}")
	public JsonResponse getRequestReviewList(@PathVariable int id) {
		JsonResponse jr = null;
		Optional<User> user = userRepo.findById(id);
		
		if (user.isPresent()) {
			List<Request> request = requestRepo.findAllByStatusAndUserNot("Review", user);
			if (!request.isEmpty()) {
				jr = JsonResponse.getInstance(request);
			} else {
				jr = JsonResponse.getErrorInstance("No requests found in for id: " + id);
			}
		} else {
			jr = JsonResponse.getErrorInstance("No user exists for id: " + id);
		}
		return jr;
	}
}
