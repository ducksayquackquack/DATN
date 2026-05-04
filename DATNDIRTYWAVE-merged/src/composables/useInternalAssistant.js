import { ref } from "vue";

const open = ref(false);
const role = ref("EMPLOYEE");
const context = ref({
  pageType: "GENERAL_INTERNAL",
  route: "",
});

export function useInternalAssistant() {
  return {
    open,
    role,
    context,
  };
}