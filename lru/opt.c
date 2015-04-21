/*
 * Source opt.c simulate OPT oracle-based optimal offline replacement
 * algorithm.
 *                                            Author: Song Jiang
 *					           sjiang@cs.wm.edu
 *                                            Revised at Nov 15, 2002
 */

#include "sim.h"

main(int argc, char* argv[])
{
  FILE *trace_fp, *cuv_fp, *para_fp;
  unsigned long i;
  int opt;
  char trc_file_name[100]; 
  char para_file_name[100];
  char cuv_file_name[100];
  
  if (argc != 2){
    printf("opt: file_name_prefix[.trc] \n");
    return;
  }

  strcpy(para_file_name, argv[1]);
  strcat(para_file_name, ".par");
  para_fp = openReadFile(para_file_name);

  strcpy(trc_file_name, argv[1]);
  strcat(trc_file_name, ".trc");
  trace_fp = openReadFile(trc_file_name);


  strcpy(cuv_file_name, argv[1]);
  strcat(cuv_file_name, "_OPT.cuv");
  cuv_fp = fopen(cuv_file_name, "w");
  
  if (!get_range(trace_fp, &vm_size, &ref_trc_len)){
    printf("trace error!\n");
    return;
  }

  page_tbl = (page_struct *)calloc(vm_size+1, sizeof(page_struct));

  while (fscanf(para_fp, "%ld", &mem_size) != EOF){
    if (mem_size <= 0)
      break;

    printf("\nmem_size = %d\n", mem_size);
 
    total_ref_pg = 0;
    num_pg_flt = 0;

    fseek(trace_fp, 0, SEEK_SET);

    free_mem_size = mem_size;

    /* initialize the page table */
    for (i = 0; i <= vm_size; i++){
      page_tbl[i].fwd_distance = 0;
      page_tbl[i].ref_times = 0;
      page_tbl[i].pf_times = 0; 

      page_tbl[i].page_num = i;
      page_tbl[i].isResident = 0; 

      page_tbl[i].OPT_next = NULL;
      page_tbl[i].OPT_prev = NULL;

      page_tbl[i].recency = ref_trc_len+1;
    }

    OPT_list_head = NULL;
    OPT_list_tail = NULL;

    OPT_Repl(trace_fp);

    printf("total_ref_pg = %d  num_pg_flt = %d \nhit ratio = %2.1f\%, mem shortage ratio = %2.1f\% \n", total_ref_pg, num_pg_flt, 100-(float)num_pg_flt/total_ref_pg*100, (float)(vm_size-mem_size)/vm_size*100);

    fprintf(cuv_fp, "%5d  %2.1f\n", mem_size, 100-(float)num_pg_flt/total_ref_pg*100);
  }

  return;
}
  


OPT_Repl(FILE *trace_fp)
{
  unsigned long length_trace = 0, i;
  unsigned long ref_page;
  unsigned short *ref_string = NULL;
  unsigned long cur_ref, step;
  page_struct *temp_OPT_ptr;
  FILE * fp = fopen("OPT.rec", "w");

  length_trace = ref_trc_len;

  ref_string = (unsigned short *)calloc(length_trace, sizeof(unsigned short));
  for (i = 0; i < length_trace; i++)
    fscanf(trace_fp, "%d", &ref_string[i]);

  step = length_trace/20;
  for (cur_ref = 0; cur_ref < length_trace; cur_ref += step)
      printf(".");
  printf("\n");

  for (cur_ref = 0; cur_ref < length_trace; cur_ref++){
    if (cur_ref % step == 0){
      printf(".");
      fflush(NULL);
    }
    ref_page = ref_string[cur_ref]; 

    page_tbl[ref_page].page_num = ref_page;
    page_tbl[ref_page].ref_times++;
 
    if (ref_page > vm_size){
      printf("Wrong ref page number found: %d.\n", ref_page);
      return FALSE;
    }

    if (!page_tbl[ref_page].isResident) {  /* page fault */
      num_pg_flt++;

      if (free_mem_size == 0){             /* free the LRU page */
	OPT_list_tail->isResident = 0;  

	temp_OPT_ptr = OPT_list_tail;
	
	OPT_list_tail = OPT_list_tail->OPT_prev;
	OPT_list_tail->OPT_next = NULL;

	temp_OPT_ptr->OPT_prev = NULL;
	temp_OPT_ptr->OPT_next = NULL;
	free_mem_size++;
      }
      page_tbl[ref_page].isResident = 1;
      free_mem_size--;
    }
    else {                             /* hit in memroy */
      if (page_tbl[ref_page].OPT_prev)
	page_tbl[ref_page].OPT_prev->OPT_next = page_tbl[ref_page].OPT_next;
      else 
	OPT_list_head = page_tbl[ref_page].OPT_next; 
	

      if (page_tbl[ref_page].OPT_next)
	page_tbl[ref_page].OPT_next->OPT_prev = page_tbl[ref_page].OPT_prev;
      else{ /* hit at the queue tail */
	OPT_list_tail = page_tbl[ref_page].OPT_prev;
	if (OPT_list_tail)
	OPT_list_tail->OPT_next = NULL;
      }
    }

    temp_OPT_ptr = OPT_list_head;
    while (temp_OPT_ptr != NULL){
      temp_OPT_ptr->fwd_distance--;
      temp_OPT_ptr = temp_OPT_ptr->OPT_next;
    }

    for (i = cur_ref+1; i < length_trace; i++)
      if (ref_page == ref_string[i])
	break;

    if (i == length_trace)
      page_tbl[ref_page].fwd_distance = length_trace;
    else 
      page_tbl[ref_page].fwd_distance = i - cur_ref;

    temp_OPT_ptr = OPT_list_head;
    while (temp_OPT_ptr != NULL && page_tbl[ref_page].fwd_distance > temp_OPT_ptr->fwd_distance)
      temp_OPT_ptr = temp_OPT_ptr->OPT_next;
    
    if (!temp_OPT_ptr){  
      if (OPT_list_tail){
        OPT_list_tail->OPT_next = (page_struct *)&page_tbl[ref_page]; 
        page_tbl[ref_page].OPT_prev = OPT_list_tail;
        page_tbl[ref_page].OPT_next = NULL;
	OPT_list_tail = OPT_list_tail->OPT_next;
      }
      else{
        OPT_list_head = OPT_list_tail = (page_struct *)&page_tbl[ref_page];
        OPT_list_tail->OPT_prev = OPT_list_tail->OPT_next = NULL;
      }
    }
    else {  /* place just before "*temp_OPT_ptr" */
      page_tbl[ref_page].OPT_prev = temp_OPT_ptr->OPT_prev; 
      page_tbl[ref_page].OPT_next = temp_OPT_ptr; 
      
   
      if (!temp_OPT_ptr->OPT_prev)	
	OPT_list_head = (page_struct *)&page_tbl[ref_page]; 
      else
	temp_OPT_ptr->OPT_prev->OPT_next = (page_struct *)&page_tbl[ref_page];
      temp_OPT_ptr->OPT_prev = (page_struct *)&page_tbl[ref_page];     
    }
  }
  printf("\n");
  total_ref_pg = length_trace;
  free(ref_string);
  return;
}

