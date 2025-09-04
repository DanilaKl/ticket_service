package hometask.ticket_client.service;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

@Service
public class TemplateFillerService {
    public void insertForm(Model model, String templateFormName, Object FormDto) {
        model.addAttribute("request_form_template", templateFormName);
        model.addAttribute("dto", FormDto);
    }

    public void insertListOfItemsResponse(Model model, String descriptionTemplateName, List<?> items) {
        model.addAttribute("response_form_template", "response_forms/list_of_items");
        model.addAttribute("item_description", descriptionTemplateName);
        model.addAttribute("items", items);
    }

    public void insertSingleItemResponse(Model model, String descriptionTemplateName, Object item) {
        model.addAttribute("response_form_template", "response_forms/single_item");
        model.addAttribute("item_description", descriptionTemplateName);
        model.addAttribute("item", item);
    }
}
